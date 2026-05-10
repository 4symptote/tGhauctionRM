package com.app.server.network;

import com.app.shared.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class AuctionServer {
    private static final Logger logger = LoggerFactory.getLogger(AuctionServer.class);


    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public AuctionServer() {}

    public void startServer(int port) {
        boolean isRunning;
        // Put try-with-resources here để đóng socket tự động
        // Tạo socket server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port {}", port);
            isRunning = true;

            while (isRunning) {
                // Luôn đợi client mới kết nối
                Socket clientSocket = serverSocket.accept();

                // cơ bản là chỉ định 1 nhân viên (handler) để xử lý client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);

                // Thread riêng để xử lý nhiều client cùng lúc
                new Thread(clientHandler).start();
            }

        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage(), e);
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public static void broadcast(Response response) {
        for (ClientHandler client : clients) {
            client.sendResponse(response);
        }
    }
}
