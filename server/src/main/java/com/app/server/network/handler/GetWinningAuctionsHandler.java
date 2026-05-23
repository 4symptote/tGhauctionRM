package com.app.server.network.handler;

import com.app.server.dao.auction.AuctionDaoImpl;
import com.app.server.network.ClientHandler;
import com.app.shared.model.auction.Auction;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import java.util.List;

public class GetWinningAuctionsHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        if (client.getCurrentUser() == null || !client.getCurrentUser().canBid()) {
            return new Response(false, "Unauthorized: Only bidders can view winning auctions.", null);
        }

        String userId = client.getCurrentUser().getId();
        List<Auction> winningAuctions = AuctionDaoImpl.getInstance().getAuctionsByHighestBidderId(userId);

        return new Response(Response.ResponseType.WINNING_AUCTION_LIST, true, "Winning auctions retrieved", winningAuctions);
    }
}