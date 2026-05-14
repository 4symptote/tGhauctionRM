package com.app.server;

import com.app.server.dao.DatabaseConnection;
import com.app.server.network.AuctionServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        System.setProperty("slf4j.internal.verbosity", "ERROR");

        DatabaseConnection.getInstance();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Server shutting down");
            DatabaseConnection.getInstance().close();
        }));

        int port = 6767;
        AuctionServer server = new AuctionServer();
        server.startServer(port);
    }
}
