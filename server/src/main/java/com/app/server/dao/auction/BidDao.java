package com.app.server.dao.auction;

import com.app.shared.model.auction.BidTransaction;
import java.util.List;

public interface BidDao {
    void saveBid(BidTransaction bid);
    List<BidTransaction> getBidsForAuction(String auctionId);
    List<BidTransaction> getBidsByUser(String userId);
}