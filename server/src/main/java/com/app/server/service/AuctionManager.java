package com.app.server.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.app.shared.model.auction.Auction;

public class AuctionManager {
    private static final AuctionManager instance = new AuctionManager();  // Eager initialization
    private final Map<String, Auction> activeAuctions;
    private final ScheduledExecutorService scheduler;
    
    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        scheduler = Executors.newScheduledThreadPool(8);  // 8 threads for auction management
    }

    public static AuctionManager getInstance() {
        // Khong can check null nua vi dung Eager initialization
        return instance;
    }

    public void startAuction(Auction auction, long durationMillis) {
        activeAuctions.put(auction.getId(), auction);

        // tinhs thoi gian ket thuc
        long endTime = System.currentTimeMillis() + durationMillis;
        auction.setEndTimeMillis(endTime);

        // TODO: Save initial state to MongoDB / Database

        // Đặt lịch kiểm tra auction sau durationMillis
        scheduler.schedule(() -> checkAndClose(auction), durationMillis, TimeUnit.MILLISECONDS);
    }

    // Kiểm tra xem auction đã kết thúc chưa
    // actualEndTime là endTime đã được cập nhập (có thể bới anti snipping) nếu như endTime chưa đến (else) đặt lịch mới đến actualEndTime
    private void checkAndClose(Auction auction) {
        long currentTime = System.currentTimeMillis();
        long actualEndTime = auction.getEndTimeMillis();

        if (currentTime >= actualEndTime) {
            // Time is actually up. End it.
            concludeAuction(auction.getId());
        } else {
            //
            long deltaWaitTime = actualEndTime - currentTime;
            scheduler.schedule(() -> checkAndClose(auction), deltaWaitTime, TimeUnit.MILLISECONDS);
        }
    }

    private void concludeAuction(String auctionId) {
        // Remove it from the active map (returns the auction, or null if already removed)
        Auction finishedAuction = activeAuctions.remove(auctionId);

        if (finishedAuction != null) {
            finishedAuction.setStatus(Auction.Status.FINISHED);

            System.out.println("Auction " + auctionId + " concluded - Winner: " + finishedAuction.getHighestBidderId());

            BidService.getInstance().cleanupLock(auctionId);  // check cleanupLock()
            // TODO: save the FINAL state to database
            // TODO: broadcast winner
        }
    }

    public void addAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
    }

    public void removeAuction(String auctionId) {
        activeAuctions.remove(auctionId);
    }

    public Auction getAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }
}
