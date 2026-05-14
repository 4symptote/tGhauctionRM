package com.app.server.dao.auction;

import com.app.server.dao.DatabaseConnection;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Item;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class AuctionDaoImpl implements AuctionDao {
    private final MongoCollection<Document> collection;

    public AuctionDaoImpl() {
        this.collection = DatabaseConnection.getInstance().getDatabase().getCollection("auctions");
    }

    @Override
    public void saveAuction(Auction auction) {
        Document doc = auctionToDocument(auction);
        collection.insertOne(doc);
    }

    @Override
    public void updateAuction(Auction auction) {
        Document doc = auctionToDocument(auction);
        collection.replaceOne(eq("_id", auction.getId()), doc, new ReplaceOptions().upsert(true));
    }

    @Override
    public Auction getAuctionById(String id) {
        // todo:
        return null;
    }

    @Override
    public List<Auction> getAllActiveAuctions() {
        // todo
        return new ArrayList<>();
    }

    private Document auctionToDocument(Auction auction) {
        Item item = auction.getItem();

        // Map the nested Item object
        Document itemDoc = new Document("name", item.getName())
                .append("description", item.getDescription())
                .append("startingPrice", item.getStartingPrice())
                .append("type", item.getClass().getSimpleName());

        // Map the main Auction object
        return new Document("_id", auction.getId()) // UUID
                .append("sellerId", auction.getSellerId())
                .append("item", itemDoc)
                .append("currentPrice", auction.getCurrentPrice())
                .append("highestBidderId", auction.getHighestBidderId())
                .append("status", auction.getStatus().name())
                .append("startTime", auction.getStartTime())
                .append("endTime", auction.getEndTimeMillis());
    }
}