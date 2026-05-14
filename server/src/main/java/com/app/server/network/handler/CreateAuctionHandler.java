package com.app.server.network.handler;

import com.app.server.network.ClientHandler;
import com.app.server.service.AuctionManager;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Item;
import com.app.shared.model.item.factory.ItemFactory;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.CreateAuctionPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAuctionHandler implements RequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(CreateAuctionHandler.class);

    @Override
    public Response handle(Request request, ClientHandler client) {
        try {
            CreateAuctionPayload payload = (CreateAuctionPayload) request.payload();

            User currentUser = client.getCurrentUser();
            if (currentUser == null) {
                return new Response(false, "You must be logged in to create an auction.", null);
            }
            //
            Item item = ItemFactory.createItem(payload);
            Auction auction = new Auction(item, payload.durationMillis());

            auction.setSellerId(currentUser.getId());

            AuctionManager.getInstance().startAuction(auction, payload.durationMillis());

            logger.info("Successfully created auction {} for user {}", auction.getId(), currentUser.getUsername());


            return new Response(true, "Auction created successfully", auction);

        } catch (ClassCastException e) {
            logger.error("Invalid payload type for CREATE_AUCTION request");
            return new Response(false, "Internal Error: Invalid payload format.", null);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create item: {}", e.getMessage());
            return new Response(false, "Failed to create item: " + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Unexpected error creating auction: ", e);
            return new Response(false, "An unexpected error occurred.", null);
        }
    }
}