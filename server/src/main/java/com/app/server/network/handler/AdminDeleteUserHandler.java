package com.app.server.network.handler;

import com.app.server.dao.user.UserDaoImpl;
import com.app.server.network.ClientHandler;
import com.app.shared.model.user.Admin;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

public class AdminDeleteUserHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        if (!(client.getCurrentUser() instanceof Admin)) return new Response(false, "Unauthorized", null);

        String targetUserId = (String) request.payload();
        boolean success = UserDaoImpl.getInstance().deleteUser(targetUserId);

        if (success) {
            return new Response(Response.ResponseType.ADMIN_ACTION_SUCCESS, true, "User account eradicated.", null);
        } else {
            return new Response(false, "User ID not found in database.", null);
        }
    }
}