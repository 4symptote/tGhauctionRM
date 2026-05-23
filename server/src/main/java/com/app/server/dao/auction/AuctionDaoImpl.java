package com.app.server.dao.auction;

import com.app.server.dao.DatabaseConnection;
import com.app.server.dao.user.UserDao;
import com.app.server.dao.user.UserDaoImpl;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Item;
import com.app.shared.model.item.factory.ItemFactory;
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
        // todo: implement this / when needed
        return null;
    }

    @Override
    public List<Auction> getAuctionsBySellerId(String sellerId) {
        List<Auction> sellerAuctions = new ArrayList<>();
        for (Document doc : collection.find(eq("sellerId", sellerId))) {
            sellerAuctions.add(documentToAuction(doc));
        }
        return sellerAuctions;
    }

    @Override
    public List<Auction> getAuctionsByHighestBidderId(String bidderId) {
        List<Auction> winningAuctions = new ArrayList<>();
        for (Document doc : collection.find(eq("highestBidderId", bidderId))) {
            winningAuctions.add(documentToAuction(doc));
        }
        return winningAuctions;
    }

    @Override
    public List<Auction> getAllActiveAuctions() {
        List<Auction> activeAuctionsList = new ArrayList<>();
        // Find all auctions where the status is OPEN or RUNNING
        Document query = new Document("status", new Document("$in", Arrays.asList("OPEN", "RUNNING")));
        for (Document doc : collection.find(query)) {
            activeAuctionsList.add(documentToAuction(doc));
        }
        return activeAuctionsList;
    }

    private Auction documentToAuction(Document doc) {
        Document itemDoc = (Document) doc.get("item");
        Item item = ItemFactory.createItemFromDocument(itemDoc);

        long startTime = doc.getLong("startTime");
        long endTime = doc.getLong("endTime");

        Auction auction = new Auction(item, startTime, endTime);
        auction.setId(doc.getString("_id"));
        auction.setSellerId(doc.getString("sellerId"));
        auction.setCurrentPrice(doc.getDouble("currentPrice"));
        auction.setHighestBidderId(doc.getString("highestBidderId"));
        auction.setStatus(Auction.Status.valueOf(doc.getString("status")));

        UserDao userDaoImpl = UserDaoImpl.getInstance();
        String sName = userDaoImpl.getUserById(auction.getSellerId()).getUsername();
        auction.setSellerName(sName != null ? sName : "Unknown");

        return auction;
    }

    private Document auctionToDocument(Auction auction) {
        // Map the main Auction object
        return new Document("_id", auction.getId())
                .append("sellerName", auction.getSellerName())
                .append("sellerId", auction.getSellerId())
                .append("item", auction.getItem().toBsonDocument()) // goated
                .append("currentPrice", auction.getCurrentPrice())
                .append("highestBidderId", auction.getHighestBidderId())
                .append("status", auction.getStatus().name())
                .append("startTime", auction.getStartTime())
                .append("endTime", auction.getEndTimeMillis());
    }
}