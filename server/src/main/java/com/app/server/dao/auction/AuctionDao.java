package com.app.server.dao.auction;

import com.app.shared.model.auction.Auction;
import java.util.List;

public interface AuctionDao {
    void saveAuction(Auction auction);
    void updateAuction(Auction auction);
    Auction getAuctionById(String id);
    List<Auction> getAuctionsBySellerId(String sellerId);
    List<Auction> getAllActiveAuctions();
}