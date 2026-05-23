package com.app.server.network.handler;

import com.app.server.dao.auction.AuctionDaoImpl;
import com.app.server.network.ClientHandler;
import com.app.shared.model.auction.Auction;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import java.util.List;

public class GetSellerAuctionsHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        //check hack
        if (client.getCurrentUser() == null || !client.getCurrentUser().canSell()) {
            return new Response(false, "Unauthorized: Only sellers can view listings", null);
        }

        String sellerId = client.getCurrentUser().getId();
        List<Auction> sellerAuctions = AuctionDaoImpl.getInstance().getAuctionsBySellerId(sellerId);

        return new Response(Response.ResponseType.SELLER_AUCTION_LIST, true, "Seller auctions retrieved", sellerAuctions);
    }
}