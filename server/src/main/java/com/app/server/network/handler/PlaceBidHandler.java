package com.app.server.network.handler;

import com.app.server.dao.auction.AuctionDao;
import com.app.server.dao.auction.AuctionDaoImpl;
import com.app.server.network.AuctionServer;
import com.app.server.network.ClientHandler;
import com.app.server.service.BidService;
import com.app.shared.exception.AuctionClosedException;
import com.app.shared.exception.AuctionNotFoundException;
import com.app.shared.exception.InvalidBidException;
import com.app.shared.model.auction.Auction;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.BidPayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceBidHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(PlaceBidHandler.class);

    @Override
    public Response handle(Request request, ClientHandler client) {
        // security check
        if (client.getCurrentUser() == null) {
            logger.warn("Unauthorized bid attempt from IP: {}", client.getInetAddress());
            return new Response(false, "Unauthorized", null);
        }

        try {
            AuctionDao auctionDao = AuctionDaoImpl.getInstance();
            BidPayload payload = (BidPayload) request.payload();

            String auctionId = payload.auctionId();
            double amount = payload.bidAmount();

            // đưa cho BidService cook
            Auction updatedAuction = BidService.getInstance().placeBid(auctionId, client.getCurrentUser(), amount);
            // update auction to db
            auctionDao.updateAuction(updatedAuction);
            Response successResponse = new Response(true, "Bid placed successfully", updatedAuction);
            AuctionServer.broadcast(successResponse);

            // null tại vì broadcast ở trên đã gọi sendResponse() rồi, trả về null tránh duplicate...
            return null;

        } catch (InvalidBidException | AuctionClosedException | AuctionNotFoundException e) {
            return new Response(false, e.getMessage(), null);

        } catch (Exception e) {
            logger.error("Unexpected error in PlaceBidHandler: ", e);
            return new Response(false, "Internal Error Occurred", null);
        }
    }
}