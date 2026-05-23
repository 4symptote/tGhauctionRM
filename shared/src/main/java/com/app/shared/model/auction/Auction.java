package com.app.shared.model.auction;

import com.app.shared.model.Entity;
import com.app.shared.model.item.Item;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {

    public enum Status {
        OPEN, RUNNING, FINISHED, PAID, CANCELED
    }

    private Item item;
    private String sellerId;
    private String sellerName;

    private long startTime;
    private long endTime;

    private double currentPrice;
    private Status status;

    private String highestBidderId;
    private String highestBidderName;
    // removed bids cuz bad

    public Auction(Item item, long startTime, long endTime) {
        super();
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.status = Status.OPEN;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getSellerName() { return sellerName; }
    public String getSellerId() { return sellerId; }

    public Item getItem() { return item; } // Returns the actual Item (Electronics, Art, etc.)
    public long getStartTime() { return startTime; }
    public long getEndTimeMillis() { return endTime; }
    public double getCurrentPrice() { return currentPrice; }

    public String getHighestBidderId() { return highestBidderId; }
    public String getHighestBidderName() { return highestBidderName; }

    public Status getStatus() {
        if (status == Status.PAID || status == Status.CANCELED) {
            return status;
        }

        long now = System.currentTimeMillis();
        if (now >= endTime) {
            status = Status.FINISHED;
        } else if (now >= startTime) {
            status = Status.RUNNING;
        } else {
            status = Status.OPEN;
        }
        return status;
    }


    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public void setItem(Item item) { this.item = item; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setEndTimeMillis(long endTime) { this.endTime = endTime; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setStatus(Status status) { this.status = status; }
    public void setHighestBidderId(String highestBidderId) { this.highestBidderId = highestBidderId; }
    public void setHighestBidderName(String highestBidderName) { this.highestBidderName = highestBidderName; }

    //
    public void processNewBid(double amount, String bidderId, String bidderName) {
        this.currentPrice = amount;
        this.highestBidderId = bidderId;
        this.highestBidderName = bidderName;
        this.item.setCurrentHighestBid(amount);
    }

    @Override
    public String toString() {
        return item.getName() + " | $" + currentPrice + " | " + getStatus();
    }
}
