package com.app.shared.model.item;

import com.app.shared.model.Entity;
import org.bson.Document;

public abstract class Item extends Entity {
    protected String name, desc;
    protected double startingPrice, currentHighestBid;
    protected String sellerId;

    public Item(String name, String desc, double startingPrice, String sellerId) {
        super();
        this.name = name;
        this.desc = desc;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.sellerId = sellerId;
    }

    public abstract Document toBsonDocument();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return desc; }
    public void setDescription(String description) { this.desc = desc; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    @Override
    public String toString() {
        return "Item{" + "name=" + name + ",\ncurrentHighestBid=" + currentHighestBid + '}';
    }
}
