package com.app.server.network.handler;

import com.app.server.network.ClientHandler;
import com.app.server.service.AuctionManager;
import com.app.shared.model.auction.Auction;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import java.util.List;

public class GetAuctionsHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        List<Auction> activeAuctions = AuctionManager.getInstance().getAllActiveAuctionsList();

        return new Response(Response.ResponseType.AUCTION_LIST,true, "Auctions retrieved successfully", activeAuctions);
    }
}