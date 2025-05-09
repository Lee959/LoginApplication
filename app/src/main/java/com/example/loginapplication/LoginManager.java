package com.example.loginapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

import com.example.loginapplication.owon.sdk.util.SocketMessageListener;

/**
 * Manager class for handling login operations
 */
public class LoginManager {
    private static final String TAG = "LoginManager";

    // 默认账号密码
    private static final String DEFAULT_USERNAME = "fbadmin";
    private static final String DEFAULT_PASSWORD = "fbadmin";

    // Password attempt tracking
    private static final int MAX_PASSWORD_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MS = 5 * 60 * 1000; // 5 minutes in milliseconds

    // Map to track login attempts and account lock status
    private Map<String, Integer> loginAttempts = new HashMap<>();
    private Map<String, Long> accountLockTime = new HashMap<>();

    private Context context;
    private Handler mainHandler;
    private SocketMessageListener globalListener;

    public LoginManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Set a global message listener for all responses
     */
    public void setSocketMessageListener(SocketMessageListener listener) {
        this.globalListener = listener;
    }

    /**
     * Check if an account is locked and return the remaining lock time in milliseconds
     * Returns 0 if the account is not locked
     */
    private long getAccountLockTimeRemaining(String account) {
        if (accountLockTime.containsKey(account)) {
            long lockEndTime = accountLockTime.get(account);
            long currentTime = System.currentTimeMillis();

            if (currentTime < lockEndTime) {
                return lockEndTime - currentTime;
            } else {
                // Lock time has expired, remove the lock
                accountLockTime.remove(account);
                loginAttempts.remove(account);
            }
        }
        return 0;
    }

    /**
     * Increment login attempts for an account and lock if necessary
     * Returns true if the account is now locked
     */
    private boolean incrementLoginAttempts(String account) {
        int attempts = loginAttempts.getOrDefault(account, 0) + 1;
        loginAttempts.put(account, attempts);

        if (attempts >= MAX_PASSWORD_ATTEMPTS) {
            // Lock the account for 5 minutes
            long lockEndTime = System.currentTimeMillis() + LOCK_DURATION_MS;
            accountLockTime.put(account, lockEndTime);
            Log.d(TAG, "账户 " + account + " 由于尝试失败次数过多，账户将被锁定 5 分钟”");
            return true;
        }
        return false;
    }

    /**
     * Reset login attempts for an account on successful login
     */
    private void resetLoginAttempts(String account) {
        loginAttempts.remove(account);
        accountLockTime.remove(account);
    }

    /**
     * Perform login operation with the given credentials
     * Uses the callback pattern to handle the asynchronous response
     */
    public void loginSocket(String account, String password, final SocketMessageListener listener) {
        Log.d(TAG, "使用以下帐户登录尝试：: " + account);

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

                // Check if account exists
                if (!DEFAULT_USERNAME.equals(account)) {
                    loginResBean.setCode(301);
                    Log.d(TAG, "账户不存在 (301)");
                }
                // Check if account is locked
                else if (getAccountLockTimeRemaining(account) > 0) {
                    loginResBean.setCode(304);
                    Log.d(TAG, "账户已锁定 (304)");
                }
                // Validate credentials
                else if (DEFAULT_USERNAME.equals(account) && DEFAULT_PASSWORD.equals(password)) {
                    loginResBean.setCode(100); // Login success
                    Log.d(TAG, "登录成功 (100)");
                    resetLoginAttempts(account);
                }
                // Password error
                else {
                    boolean isNowLocked = incrementLoginAttempts(account);

                    if (isNowLocked) {
                        loginResBean.setCode(302); // Password error limit reached
                        Log.d(TAG, "密码错误次数达到上限，账号被锁定5分钟 (302)");
                    } else {
                        loginResBean.setCode(303); // Password error
                        Log.d(TAG, "密码错误 (303). Attempt " + loginAttempts.get(account) + " of " + MAX_PASSWORD_ATTEMPTS);
                    }
                }

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.getMessage(1001, loginResBean); // 1001 is the command ID for login
                        }

                        // Also notify the global listener if set
                        if (globalListener != null) {
                            globalListener.getMessage(1001, loginResBean);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * For completeness, include methods to initialize and close connections
     * These are no-ops in this simplified implementation
     */
    public void initConnection() {
        Log.d(TAG, "Connection initialized");
    }

    public void closeConnection() {
        Log.d(TAG, "Connection closed");
    }
}