package com.app.server;

import com.app.server.network.AuctionServer;

public class ServerMain {

    public static void main(String[] args) {
        System.setProperty("slf4j.internal.verbosity", "WARN"); // Dont care abt this sh lol

        int port = 8080;
        AuctionServer server = new AuctionServer();
        server.startServer(port);
    }
}
