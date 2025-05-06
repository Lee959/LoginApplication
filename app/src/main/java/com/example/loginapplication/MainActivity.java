package com.example.loginapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.loginapplication.owon.sdk.util.SocketMessageListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private TextView receiveMessage;
    private ProgressBar progressBar;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI component
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        receiveMessage = findViewById(R.id.receive_message);
        progressBar = findViewById(R.id.progressBar);
        loginManager = new LoginManager(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // Validate input
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                receiveMessage.setText("Connecting...");

                performLogin(username, password);
            }
        });
    }

    private void performLogin(String username, String password) {
        Log.d(TAG, "Attempting login with username: " + username);

        loginManager.loginSocket(username, password, new SocketMessageListener() {
            @Override
            public void getMessage(int commandID, Object bean) {
                processLoginResponse(commandID, bean);
            }
        });
    }

    private void processLoginResponse(int commandID, Object bean) {
        progressBar.setVisibility(View.GONE);

        if (bean instanceof LoginSocketResBean) {
            LoginSocketResBean loginRes = (LoginSocketResBean) bean;
            int code = loginRes.getCode();

            JSONObject jsonResponse = new JSONObject();
            try {
                jsonResponse.put("commandID", commandID);
                jsonResponse.put("code", code);
                jsonResponse.put("message", getLoginStatusMessage(code));

                // Log the JSON response
                String jsonString = jsonResponse.toString();
                Log.d(TAG, "Login Response: " + jsonString);

                // Update UI
                receiveMessage.setText(getLoginStatusMessage(code));

                if (code == 100) {
                    Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Login Failed: " + getLoginStatusMessage(code), Toast.LENGTH_SHORT).show();
                }

            } catch (JSONException e) {
                Log.e(TAG, "JSON Error: " + e.getMessage());
            }
        }
    }

    private String getLoginStatusMessage(int code) {
        switch (code) {
            case 100:
                return "登录成功";
            case 110:
                return "登录失败";
            case 301:
                return "账号不存在";
            case 302:
                return "密码错误次数达到上限，账号被锁定5分钟";
            case 303:
                return "密码错误";
            case 304:
                return "账号被锁定";
            default://This can be delete
                return "未知错误";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the Netty connection when the activity is destroyed
        if (loginManager != null) {
            loginManager.closeConnection();
        }
    }
}