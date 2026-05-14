package com.app.server.network.handler;

import com.app.server.network.ClientHandler;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogoutHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(LogoutHandler.class);

    @Override
    public Response handle(Request request, ClientHandler client) {
        if (client.getCurrentUser() != null) {
            //logger.info("User logged out: {}", client.getCurrentUser().getUsername());

            client.setCurrentUser(null);
        }

        return new Response(true, "Logged out successfully.", null);
    }
}