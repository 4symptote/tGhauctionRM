package com.app.client.network;

import com.app.shared.network.Response;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NetworkClient {
    private static final Logger logger = LoggerFactory.getLogger(NetworkClient.class);

    private static NetworkClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // listeners = observers:
    // List of observers that implements the ResponseListener interface:
    private final List<ResponseListener> listeners = new CopyOnWriteArrayList<>();

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

            logger.info("Connected to server");
        } catch (Exception e) {
            logger.error("Failed to connect to server");
        }
    }


    public void sendRequest(com.app.shared.network.Request request) {
        if (!isConnected()) {
            logger.error("Not connected to server.");
            return;
        }
        try {
            out.writeObject(request);
            out.flush();
            logger.info("Sent {}", request.type());
        } catch (IOException e) {
            logger.error("Failed to send request: {}", e.getMessage());
            disconnect();
        }
    }

    public void startListener() {
        Thread listenerThread = new Thread(() -> {
            while (isConnected()) {
                try {
                    // Block and wait for a message from the server
                    Response response = (Response) in.readObject();
                    handleResponse(response);
                } catch (Exception e) {
                    if (!isConnected()) {
                        logger.error("Connection lost: {}", e.getMessage());
                        disconnect();
                    }
                }
            }
        });
        // A daemon thread will automatically die when the JavaFX app closes
        listenerThread.setDaemon(true);
        listenerThread.start();
    }


    public void addListener(ResponseListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ResponseListener listener) {
        listeners.remove(listener);
    }

    // listen = observe
    // Broadcasting = Notifying to all observers:
    private void handleResponse(Response response) {
        // todo: xemthem Platform.runLater()
        Platform.runLater(() -> {
            // Notify all observers
            for (ResponseListener listener : listeners) {
                listener.onResponseReceived(response);
            }
        });
    }


    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            logger.info("Disconnected from server.");
        } catch (IOException e) {
            logger.error("Error while disconnecting: {}", e.getMessage());
        }
    }
}
