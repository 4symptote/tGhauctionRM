package com.app.server.dao.auction;

import com.app.server.dao.DatabaseConnection;
import com.app.shared.model.auction.BidTransaction;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import static com.mongodb.client.model.Filters.eq;

public class BidDaoImpl implements BidDao {
    private static final BidDaoImpl instance = new BidDaoImpl();
    private final MongoCollection<Document> collection;

    private BidDaoImpl() {
        this.collection = DatabaseConnection.getInstance().getDatabase().getCollection("bids");
    }

    public static BidDaoImpl getInstance() { return instance; }

    @Override
    public void saveBid(BidTransaction bid) {
        Document doc = new Document("_id", bid.id())
                .append("auctionId", bid.auctionId())
                .append("bidderId", bid.bidderId())
                .append("amount", bid.amount())
                .append("timestamp", bid.timestamp());

        collection.insertOne(doc);
    }

    @Override
    public List<BidTransaction> getBidsForAuction(String auctionId) {
        List<BidTransaction> history = new ArrayList<>();
        // todo: when have chart
        for (Document doc : collection.find(eq("auctionId", auctionId)).sort(new Document("timestamp", 1))) {
            history.add(documentToBid(doc));
        }
        return history;
    }

    @Override
    public List<BidTransaction> getBidsByUser(String userId) {
        List<BidTransaction> history = new ArrayList<>();
        for (Document doc : collection.find(eq("bidderId", userId))) {
            history.add(documentToBid(doc));
        }
        return history;
    }

    private BidTransaction documentToBid(Document doc) {
        return new BidTransaction(
                doc.getString("_id"),
                doc.getString("auctionId"),
                doc.getString("bidderId"),
                doc.getDouble("amount"),
                doc.getLong("timestamp")
        );
    }
}