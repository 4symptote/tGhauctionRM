package com.app.server.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.net.Socket;


public class AuctionServer {
    private static final Logger logger = LoggerFactory.getLogger(AuctionServer.class);
    private boolean isRunning;
    public AuctionServer() {}

    public void startServer(int port) {
        // Put try-with-resources here để đóng socket tự động
        // Tạo socket server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
        // If successful:
            System.out.println("Server started on port " + port);
            isRunning = true;

            while (isRunning) {
                // Luôn đợi client mới kết nối
                Socket client = serverSocket.accept();
                // In ra nếu client mới kết nối
                System.out.println("Client connected: " + client.getInetAddress());
            }

        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
        }
    }

}
