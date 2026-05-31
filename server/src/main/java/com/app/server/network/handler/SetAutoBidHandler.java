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

            boolean wasUpdate = AutoBidService.getInstance().hasAutoBid(currentUser.getId(), payload.auctionId());

            AutoBidService.getInstance().setOrUpdateAutoBid(currentUser.getId(), payload.auctionId(), payload.maxLimit());

            // update user
            User updatedUser = UserDaoImpl.getInstance().getUserById(currentUser.getId());
            client.setCurrentUser(updatedUser);

            String successMsg = wasUpdate ?
                    "Successfully updated your Auto-Bid limit to $" + payload.maxLimit() + "!" :
                    "Successfully locked $" + payload.maxLimit() + " in escrow. The system will now bid on your behalf!";

            return new Response(Response.ResponseType.AUTO_BID_SET, true, successMsg, updatedUser);

        } catch (IllegalStateException e) {
            return new Response(false, e.getMessage(), null); // Insufficient funds or cannot lower limit
        } catch (Exception e) {
            return new Response(false, "Failed to set Auto-Bid", null);
        }
    }
}