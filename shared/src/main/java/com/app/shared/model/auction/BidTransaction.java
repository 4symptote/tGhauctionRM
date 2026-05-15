package com.app.shared.model.auction;

import java.io.Serializable;
import java.util.UUID;

public record BidTransaction(

        String id,
        String auctionId,
        String bidderId,
        double amount,
        long timestamp

) implements Serializable {
    // constructor for creating a new BidTransaction, with a new randomized unique id
    public BidTransaction(String auctionId, String bidderId, double amount) {
        this(UUID.randomUUID().toString(), auctionId, bidderId, amount, System.currentTimeMillis());
    }
    // Constructor de tao BidTransaction tu database
    // BidTransaction(id, auctionId, bidderId, amount, timestamp)
    // ko can vi day la Record
    // rat dep <-- Important
}