package com.app.server.network.handler;

import com.app.server.network.AuctionServer;
import com.app.server.network.ClientHandler;
import com.app.server.service.AuctionManager;
import com.app.shared.model.user.Admin;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

public class AdminDeleteAuctionHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        if (!(client.getCurrentUser() instanceof Admin)) return new Response(false, "Unauthorized", null);

        String auctionId = (String) request.payload();
        AuctionManager.getInstance().forceDeleteAuction(auctionId);

        AuctionServer.broadcast(
                new Response(Response.ResponseType.AUCTION_UPDATED, true, "Admin deleted an auction.", null)
        );

        return new Response(Response.ResponseType.ADMIN_ACTION_SUCCESS, true, "Auction permanently deleted.", null);
    }
}