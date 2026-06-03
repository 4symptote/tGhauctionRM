package com.app.server.service;

import com.app.server.dao.user.UserDaoImpl;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.user.User;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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

    private final Map<String, ReentrantLock> engineLocks = new ConcurrentHashMap<>();

    private AutoBidService() {}

    public static AutoBidService getInstance() { return instance; }

    public void setOrUpdateAutoBid(String userId, String auctionId, double newMaxLimit) {
        ReentrantLock lock = engineLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
        lock.lock();
        try {
            Map<String, BidState> states = autoBidLimits.computeIfAbsent(auctionId, k -> new ConcurrentHashMap<>());
            BidState state = states.get(userId);

            if (state == null) {
                boolean success = UserDaoImpl.getInstance().lockFunds(userId, newMaxLimit);
                if (success) {
                    states.put(userId, new BidState(newMaxLimit, newMaxLimit));
                    logger.info("Auto-Bid registered for user {} on auction {} (Limit: ${})", userId, auctionId, newMaxLimit);
                } else {
                    throw new IllegalStateException("Insufficient balance to lock Auto-Bid funds.");
                }
            } else {
                // update existing auto
                double difference = newMaxLimit - state.maxLimit;

                if (difference > 0) {
                    // lock extra funds
                    boolean success = UserDaoImpl.getInstance().lockFunds(userId, difference);
                    if (!success) {
                        throw new IllegalStateException("Insufficient balance to increase Auto-Bid limit.");
                    }
                } else if (difference < 0) {
                    // unlock difference funds
                    double amountToUnlock = Math.abs(difference);

                    if (amountToUnlock > state.currentlyLocked) {
                        throw new IllegalStateException("Cannot lower limit to $" + newMaxLimit + " because your active bids already exceed this amount.");
                    }
                    UserDaoImpl.getInstance().unlockFunds(userId, amountToUnlock);
                }

                state.maxLimit = newMaxLimit;
                state.currentlyLocked += difference;
                logger.info("Auto-Bid updated for user {} on auction {} (New Limit: ${})", userId, auctionId, newMaxLimit);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean hasAutoBid(String userId, String auctionId) {
        Map<String, BidState> states = autoBidLimits.get(auctionId);
        return states != null && states.containsKey(userId);
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