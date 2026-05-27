package com.app.server.service;

import com.app.server.dao.user.UserDaoImpl;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.user.User;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoBidService {
    private static final Logger logger = LoggerFactory.getLogger(AutoBidService.class);
    private static final AutoBidService instance = new AutoBidService();

    private static class BidState {
        double maxLimit;
        double currentlyLocked;
        BidState(double maxLimit, double currentlyLocked) {
            this.maxLimit = maxLimit;
            this.currentlyLocked = currentlyLocked;
        }
    }

    // Map: auctionId -> (userId -> BidState)
    private final Map<String, Map<String, BidState>> autoBidLimits = new ConcurrentHashMap<>();

    private AutoBidService() {}

    public static AutoBidService getInstance() { return instance; }

    public void registerAutoBid(String userId, String auctionId, double maxLimit) {
        boolean success = UserDaoImpl.getInstance().lockFunds(userId, maxLimit);
        if (success) {
            autoBidLimits.computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>())
                    .put(userId, new BidState(maxLimit, maxLimit));
            logger.info("Auto-Bid registered for user {} on auction {} (Max Limit: ${})", userId, auctionId, maxLimit);
        } else {
            throw new IllegalStateException("Insufficient balance to lock Auto-Bid funds.");
        }
    }

    public void evaluate(String auctionId) {
        Map<String, BidState> states = autoBidLimits.get(auctionId);
        if (states == null || states.isEmpty()) return;

        Auction auction = AuctionManager.getInstance().getAuction(auctionId);
        if (auction == null || auction.getStatus() != Auction.Status.RUNNING) return;

        String currentWinner = auction.getHighestBidderId();
        double nextBidAmount = auction.getCurrentPrice() + 10.0; // 10 inc

        String bestBidderId = null;

        for (Map.Entry<String, BidState> entry : states.entrySet()) {
            String userId = entry.getKey();
            BidState state = entry.getValue();

            // Check if they can afford it using their TOTAL Max Limit, not just the locked
            if (!userId.equals(currentWinner) && state.maxLimit >= nextBidAmount) {
                bestBidderId = userId;
                break;
            }
        }

        if (bestBidderId != null) {
            final String bidderToExecute = bestBidderId;

            CompletableFuture.runAsync(() -> {
                BidState state = states.get(bidderToExecute);
                try {
                    // unlock funds, gộp phần reversed còn lại với phần đc refund mới => full money để bid tiếp
                    UserDaoImpl.getInstance().unlockFunds(bidderToExecute, state.currentlyLocked);

                    // đặt bid trực tiếp
                    User bidder = UserDaoImpl.getInstance().getUserById(bidderToExecute);
                    BidService.getInstance().placeBid(auctionId, bidder, nextBidAmount);

                    // lock lại những j còn lại sau khi bid từ max limit
                    double newLockedAmount = state.maxLimit - nextBidAmount;
                    if (newLockedAmount > 0) {
                        UserDaoImpl.getInstance().lockFunds(bidderToExecute, newLockedAmount);
                        state.currentlyLocked = newLockedAmount;
                    } else {
                        state.currentlyLocked = 0; // They have exactly hit their ceiling
                    }

                } catch (Exception e) {
                    // If the bid fails, safely lock the exact amount we just unlocked
                    UserDaoImpl.getInstance().lockFunds(bidderToExecute, state.currentlyLocked);
                }
            });
        }
    }

    public void releaseAllEscrow(String auctionId) {
        Map<String, BidState> states = autoBidLimits.remove(auctionId);
        if (states != null) {
            for (Map.Entry<String, BidState> entry : states.entrySet()) {
                // Unlock tất cả (khi auction end chẳng hạn)
                UserDaoImpl.getInstance().unlockFunds(entry.getKey(), entry.getValue().currentlyLocked);
            }
        }
    }
}