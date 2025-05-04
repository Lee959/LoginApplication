package com.example.loginapplication.netty;

import android.util.Log;

import com.example.loginapplication.LoginSocketResBean;
import com.example.loginapplication.owon.sdk.util.SocketMessageListener;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class NettyClient {
    private static final String TAG = "NettyClient";

    private static final String HOST = "example.server.com"; // Replace with your server address
    private static final int PORT = 8080; // Replace with your server port

    private EventLoopGroup group;
    private Channel channel;
    private SocketMessageListener messageListener;

    public NettyClient(SocketMessageListener listener) {
        this.messageListener = listener;
    }

    public void connect() {
        group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new NettyClientHandler());
                        }
                    });

            // Connect to the server
            ChannelFuture future = bootstrap.connect(HOST, PORT).sync();

            if (future.isSuccess()) {
                channel = future.channel();
                Log.d(TAG, "Connected to server successfully");
            } else {
                Log.e(TAG, "Failed to connect to server");
            }
        } catch (Exception e) {
            Log.e(TAG, "Connection exception: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    public void login(String username, String password) {
        if (channel != null && channel.isActive()) {
            try {
                // Create JSON login request
                JSONObject loginRequest = new JSONObject();
                loginRequest.put("command", "LOGIN");
                loginRequest.put("username", username);
                loginRequest.put("password", password);

                // Send the login request
                channel.writeAndFlush(loginRequest.toString());
                Log.d(TAG, "Login request sent: " + loginRequest.toString());
            } catch (JSONException e) {
                Log.e(TAG, "JSON error: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Channel is not active. Cannot send login request.");

            // Notify about connection failure
            if (messageListener != null) {
                LoginSocketResBean resBean = new LoginSocketResBean();
                resBean.setCode(110); // Login failed due to connection issue
                messageListener.getMessage(1001, resBean);
            }
        }
    }

    // Inner class for handling incoming messages
    private class NettyClientHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            Log.d(TAG, "Received message: " + msg);

            try {
                // Parse the received JSON message
                JSONObject jsonResponse = new JSONObject(msg);

                if (jsonResponse.has("command") && "LOGIN_RESPONSE".equals(jsonResponse.getString("command"))) {
                    // Create response bean
                    LoginSocketResBean resBean = new LoginSocketResBean();
                    resBean.setCode(jsonResponse.getInt("code"));

                    // Notify through the callback interface
                    if (messageListener != null) {
                        messageListener.getMessage(1001, resBean);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.getMessage());
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            Log.d(TAG, "Channel active: " + ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            Log.d(TAG, "Channel inactive");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            Log.e(TAG, "Exception caught: " + cause.getMessage());
            ctx.close();
        }
    }
}