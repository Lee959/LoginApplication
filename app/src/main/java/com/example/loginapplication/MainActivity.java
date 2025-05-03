package com.example.loginapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.loginapplication.owon.sdk.util.SocketMessageListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private TextView receiveMessage;

    // 默认账号密码
    private static final String DEFAULT_USERNAME = "fbadmin";
    private static final String DEFAULT_PASSWORD = "fbadmin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        receiveMessage = findViewById(R.id.receive_message);

        btnLogin.setText("Login");

        // Set username hint instead of text
        etUsername.setHint("Please Enter your Username......");
        etUsername.setText("");

        // Set password hint
        etPassword.setHint("Please Enter your Password");

        // Set click listener for login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // Perform login
                performLogin(username, password);
            }
        });
    }

    private void performLogin(String username, String password) {
        Log.d(TAG, "Attempting login with username: " + username);

        // Create a login socket handler instance
        LoginSocketHandler loginHandler = new LoginSocketHandler(this);

        // Call login method and handle the response through callback
        loginHandler.loginSocket(username, password, new SocketMessageListener() {
            @Override
            public void getMessage(int commandID, Object bean) {
                // Process login response
                processLoginResponse(commandID, bean);
            }
        });
    }

    private void processLoginResponse(int commandID, Object bean) {
        if (bean instanceof LoginSocketResBean) {
            LoginSocketResBean loginRes = (LoginSocketResBean) bean;
            int code = loginRes.getCode();

            // Create JSON response for logging
            JSONObject jsonResponse = new JSONObject();
            try {
                jsonResponse.put("commandID", commandID);
                jsonResponse.put("code", code);
                jsonResponse.put("message", getLoginStatusMessage(code));

                // Log the JSON response
                String jsonString = jsonResponse.toString();
                Log.d(TAG, "Login Response: " + jsonString);

                // Update UI on main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        receiveMessage.setText(getLoginStatusMessage(code));

                        if (code == 100) {
                            Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Login Failed: " + getLoginStatusMessage(code), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            } catch (JSONException e) {
                Log.e(TAG, "JSON Error: " + e.getMessage());
            }
        }
    }

    private String getLoginStatusMessage(int code) {
        switch (code) {
            case 100:
                return "Login successful";
            case 110:
                return "Login failed";
            case 301:
                return "Account does not exist";
            case 302:
                return "Password error limit reached, account locked for 5 minutes";
            case 303:
                return "Password error";
            case 304:
                return "Account locked";
            default:
                return "Unknown error";
        }
    }
}