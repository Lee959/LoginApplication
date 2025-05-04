package com.example.loginapplication.owon.sdk.util;

/**
 * Interface for socket message callbacks
 */
public interface SocketMessageListener {
    /**
     * Method called when a message is received from the socket connection
     *
     * @param commandID The command identifier
     * @param bean The response data object
     */
    void getMessage(int commandID, Object bean);
}