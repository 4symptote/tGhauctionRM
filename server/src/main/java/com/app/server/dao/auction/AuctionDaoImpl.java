package com.app.server.dao.auction;

import com.app.server.dao.DatabaseConnection;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Art;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.item.Item;
import com.app.shared.model.item.Vehicle;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class AuctionDaoImpl implements AuctionDao {
    private static final AuctionDaoImpl instance = new AuctionDaoImpl();
    private final MongoCollection<Document> collection;

    private AuctionDaoImpl() {
        this.collection = DatabaseConnection.getInstance().getDatabase().getCollection("auctions");
    }

    public static AuctionDaoImpl getInstance() {
        return instance;
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
        List<Auction> activeAuctionsList = new ArrayList<>();

        // Find all auctions where the status is OPEN or RUNNING
        Document query = new Document("status", new Document("$in", Arrays.asList("OPEN", "RUNNING")));

        for (Document doc : collection.find(query)) {
            // 1. Rebuild the Item
            Document itemDoc = (Document) doc.get("item");
            String type = itemDoc.getString("type");

            Item item;
            switch (type) {
                case "Art" -> item = new Art.Builder().build();
                case "Vehicle" -> item = new Vehicle.Builder().build();
                default -> item = new Electronics.Builder().build();
            }

            item.setName(itemDoc.getString("name"));
            item.setDescription(itemDoc.getString("description"));
            item.setStartingPrice(itemDoc.getDouble("startingPrice"));

            // 2. Rebuild the Auction
            long startTime = doc.getLong("startTime");
            long endTime = doc.getLong("endTime");

            Auction auction = new Auction(item, startTime, endTime);

            // Overwrite the automatically generated ID with the real one from the DB
            auction.setId(doc.getString("_id"));
            auction.setSellerId(doc.getString("sellerId"));
            auction.setCurrentPrice(doc.getDouble("currentPrice"));
            auction.setHighestBidderId(doc.getString("highestBidderId"));
            auction.setStatus(Auction.Status.valueOf(doc.getString("status")));

            activeAuctionsList.add(auction);
        }

        return activeAuctionsList;
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
                .append("sellerName", auction.getSellerName())
                .append("sellerId", auction.getSellerId())
                .append("item", itemDoc)
                .append("currentPrice", auction.getCurrentPrice())
                .append("highestBidderId", auction.getHighestBidderId())
                .append("status", auction.getStatus().name())
                .append("startTime", auction.getStartTime())
                .append("endTime", auction.getEndTimeMillis());
    }
}