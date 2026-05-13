package com.app.server.network;

import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.slf4j.Logger;

public class ClientHandler implements Runnable {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning;

    private final RequestRouter requestRouter = new RequestRouter();
    // the User with this connection
    // dang nhap xong or register xong se cap nhat vao currentUser
    private User currentUser;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            isRunning = true;

            logger.info("Info: New client connected: {}", socket.getInetAddress());

            // listen to client's requests
            while (isRunning) {
                Request request = (Request) in.readObject();

                // and handle them
                Response response = requestRouter.route(request, this);
                if (response != null) {
                    sendResponse(response);
                }
            }

        } catch (EOFException e) {
            logger.info("Info: Client disconnected gracefully: {}", socket.getInetAddress());
        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void sendResponse(Response response) {
        try {
            out.writeObject(response);
            out.flush();
            out.reset();
        } catch (IOException e) {
            logger.error("Error: Failed to send response: {}", e.getMessage());
            disconnect();
        }
    }

    private void disconnect() {
        isRunning = false;
        try {
            java.net.InetAddress clientAddress = socket.getInetAddress();
            if (in != null) in.close();
            if (out != null) out.close();
            socket.close();
            AuctionServer.removeClient(this);
            logger.info("Info: Client disconnected elegantly: {}", clientAddress);
        } catch (IOException e) {
            logger.error("Error: error occurred while trying to disconnect: {}", e.getMessage());
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public java.net.InetAddress getInetAddress() {
        return socket.getInetAddress();
    }
}