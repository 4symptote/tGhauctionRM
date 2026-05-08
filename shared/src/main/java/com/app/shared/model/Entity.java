package com.app.shared.model;

import java.io.Serializable;
import java.util.UUID;

// Mỗi một entity (Item, Bidder, Seller, Auction) đều có một ID duy nhất.

public abstract class Entity implements Serializable {
    protected String id;

    public Entity() {
        this.id = UUID.randomUUID().toString();
    }

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }

}

// Entity <- item, user, auction

// item   : Vehicle, Art, Electronics, Clothing, etc.
// user   : Bidder, Seller, Admin
// auction: Auction, BidTransaction