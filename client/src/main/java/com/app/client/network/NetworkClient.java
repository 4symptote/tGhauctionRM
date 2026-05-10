package com.app.client.network;

import com.app.shared.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkClient {
    private static final Logger logger = LoggerFactory.getLogger(NetworkClient.class);

    private static NetworkClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private NetworkClient() {}

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && out != null && in != null;
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            logger.info("Info: Connected to server");
        } catch (Exception e) {
            logger.error("Error: Failed to connect to server: {}", e.getMessage());
        }
    }
    public void startListener() {
        Thread listenerThread = new Thread(() -> {
            while (isConnected()) {
                try {
                    // Block and wait for a message from the server
                    Response response = (Response) in.readObject();
                    //handleServerResponse(response);
                } catch (Exception e) {
                    if (!isConnected()) {
                        logger.error("Error: Connection lost: {}", e.getMessage());
                        disconnect();
                    }
                }
            }
        });
        // A daemon thread will automatically die when the JavaFX app closes
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            logger.info("Info: Disconnected from server.");
        } catch (IOException e) {
            logger.error("Error: Error while disconnecting: {}", e.getMessage());
        }
    }
}
