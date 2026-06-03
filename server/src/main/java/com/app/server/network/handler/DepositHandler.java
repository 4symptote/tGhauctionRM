package com.app.server.network.handler;

import com.app.server.dao.user.UserDaoImpl;
import com.app.server.network.ClientHandler;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

public class DepositHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        User currentUser = client.getCurrentUser();

        if (currentUser == null || !currentUser.canBid()) {
            return new Response(false, "only bidders can deposit", null);
        }

        double amount = (Double) request.payload();
        UserDaoImpl.getInstance().deposit(currentUser.getId(), amount);

        User updatedUser = UserDaoImpl.getInstance().getUserById(currentUser.getId());
        client.setCurrentUser(updatedUser);

        return new Response(Response.ResponseType.USER_UPDATED, true, "Deposit successful", updatedUser);
    }
}