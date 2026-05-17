package com.app.server.network.handler;

import com.app.server.dao.auction.BidDaoImpl;
import com.app.server.network.ClientHandler;
import com.app.shared.model.auction.BidTransaction;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

import java.util.List;

public class GetBidHistoryHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        try {
            // Request(GET_BID_HISTORY, auctionId)
            // payload: auctionId
            String auctionId = (String) request.payload();

            // fetch sorted bids
            List<BidTransaction> history = BidDaoImpl.getInstance().getBidsForAuction(auctionId);

            return new Response(true, "BID_HISTORY", history);
        } catch (Exception e) {
            return new Response(false, "Failed to fetch bid history.", null);
        }
    }
}