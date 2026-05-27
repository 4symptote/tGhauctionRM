package com.app.server.network.handler;

import com.app.server.dao.user.UserDaoImpl;
import com.app.server.network.ClientHandler;
import com.app.server.service.AutoBidService;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.AutoBidPayload;

public class SetAutoBidHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        User currentUser = client.getCurrentUser();
        if (currentUser == null || !currentUser.canBid()) {
            return new Response(false, "Only bidders can set auto-bids", null);
        }

        try {
            AutoBidPayload payload = (AutoBidPayload) request.payload();
            AutoBidService.getInstance().registerAutoBid(currentUser.getId(), payload.auctionId(), payload.maxLimit());

            // update user
            User updatedUser = UserDaoImpl.getInstance().getUserById(currentUser.getId());
            client.setCurrentUser(updatedUser);

            return new Response(Response.ResponseType.AUTO_BID_SET, true, "Auto-Bid set", updatedUser);

        } catch (IllegalStateException e) {
            return new Response(false, e.getMessage(), null); // Insufficient funds
        } catch (Exception e) {
            return new Response(false, "Failed to set Auto-Bid", null);
        }
    }
}