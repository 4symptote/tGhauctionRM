package com.app.server.network.handler;

import com.app.server.network.ClientHandler;
import com.app.server.service.UserService;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

import com.app.shared.network.payload.RegisterPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(RegisterHandler.class);

    @Override
    public Response handle(Request request, ClientHandler client) {
        // if already logged in
        if (client.getCurrentUser() != null) {
            return new Response(false, "Da dang nhap roi", null);
        }

        try {
            // Unpack the payload
            RegisterPayload payload = (RegisterPayload) request.payload();

            // dua cho service cook, fail -> exception
            User newUser = UserService.getInstance().register(payload);

            // .
            client.setCurrentUser(newUser);

            logger.info("New user {} registered from: {}",
                    client.getInetAddress(), newUser.getUsername());

            return new Response(true, "Registered Successfully", newUser);

        } catch (ClassCastException e) {
            logger.error("Error: Payload casting error");
            return new Response(false, "Loi du lieu", null);

        } catch (IllegalArgumentException e) {
            // catch exception khi UserService.register() nems
            logger.warn("Failed register attempt from IP {}: {}", client.getInetAddress(), e.getMessage());
            return new Response(false, e.getMessage(), null);

        } catch (Exception e) {
            logger.error("Unexpected error during registration: ", e);
            return new Response(false, "?? Error", null);
        }
    }
}