package com.app.server.network.handler;

import com.app.server.dao.user.UserDaoImpl;
import com.app.server.network.ClientHandler;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

public class WithdrawHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        User currentUser = client.getCurrentUser();

        if (currentUser == null || !currentUser.canSell()) {
            return new Response(false, "only sellers can withdraw", null); // for now
        }

        double amount = (Double) request.payload();
        boolean success = UserDaoImpl.getInstance().withdraw(currentUser.getId(), amount);

        if (success) {
            User updatedUser = UserDaoImpl.getInstance().getUserById(currentUser.getId());
            client.setCurrentUser(updatedUser);
            return new Response(Response.ResponseType.USER_UPDATED, true, "withdrawal successful", updatedUser);
        } else {
            return new Response(false, "you hav no money to withdraw", null);
        }
    }
}