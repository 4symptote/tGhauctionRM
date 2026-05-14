package com.app.server.network.handler;

import com.app.server.network.ClientHandler;
import com.app.server.service.UserService;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.LoginPayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);

    @Override
    public Response handle(Request request, ClientHandler client) {
        // double login
        if (client.getCurrentUser() != null) {
            return new Response(false, "chill out", null);
        }

        try {
            // unpack payload
            LoginPayload payload = (LoginPayload) request.payload();

            // dua cho service cook, fail -> exception
            User authenticatedUser = UserService.getInstance().login(payload);

            // !!!! ClientHandler now knows who the client actually is
            client.setCurrentUser(authenticatedUser);

            logger.info("{} logged from: {}",
                    authenticatedUser.getUsername(), client.getInetAddress());
            logger.info("Id: {}", authenticatedUser.getId());

            // return success response + tk User
            return new Response(true, "Logged in successfully", authenticatedUser);

        } catch (ClassCastException e) {
            logger.error("Error: Payload casting error:");
            e.printStackTrace();
            return new Response(false, "Loi du lieu", null);

        } catch (IllegalArgumentException e) {
            // catch exception khi UserService.login() nem
            logger.warn("Failed login attempt from IP {}: {}", client.getInetAddress(), e.getMessage());
            return new Response(false, e.getMessage(), null);

        } catch (Exception e) {
            logger.error("Unexpected error during login: ", e);
            return new Response(false, "?? Error", null);
        }
    }
}
