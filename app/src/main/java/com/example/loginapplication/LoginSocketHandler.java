package com.example.loginapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.loginapplication.owon.sdk.util.SocketMessageListener;

public class LoginSocketHandler {
    private static final String TAG = "LoginSocketHandler";
    private Context context;
    private Handler handler;

    public LoginSocketHandler(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }

    // Method to perform socket login
    public void loginSocket(String account, String password, SocketMessageListener listener) {
        Log.d(TAG, "Establishing socket connection...");

        // Create a new thread to simulate network operation
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Simulate network delay
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                final LoginSocketResBean loginResBean = new LoginSocketResBean();
                // checking password
                if ("fbadmin".equals(account) && "fbadmin".equals(password)) {
                    loginResBean.setCode(100); // Login success
                    Log.d(TAG, "Login successful (100)");
                } else if (!"fbadmin".equals(account)) {
                    loginResBean.setCode(301); // 账号不存在
                    Log.d(TAG, "账号不存在  (301)");
                } else {
                    loginResBean.setCode(110); // 登录失败
                    Log.d(TAG, "登陆失败 (110)");
                }

                // Post callback to main thread
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.getMessage(1001, loginResBean); // 1001 is simulated command ID for login
                        }
                    }
                });
            }
        }).start();
    }
}
