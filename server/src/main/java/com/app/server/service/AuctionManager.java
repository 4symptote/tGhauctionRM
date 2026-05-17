package com.app.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.app.server.dao.auction.AuctionDao;
import com.app.server.dao.auction.AuctionDaoImpl;
import com.app.shared.model.auction.Auction;

import com.app.shared.network.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionManager {
    private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);

    private static AuctionManager instance;// = AuctionManager.getInstance();  // Eager initialization
    private final Map<String, Auction> activeAuctions;
    private final ScheduledExecutorService scheduler;

    private final AuctionDao auctionDao = AuctionDaoImpl.getInstance();
    
    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        scheduler = Executors.newScheduledThreadPool(8);  // 8 threads for auction management
        loadAuctionsFromDatabase();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void startAuction(Auction auction, long durationMillis) {
        activeAuctions.put(auction.getId(), auction);

        // tinhs thoi gian ket thuc
        long endTime = System.currentTimeMillis() + durationMillis;
        auction.setEndTimeMillis(endTime);

        // TODO: Save initial state to MongoDB / Database
        auctionDao.saveAuction(auction);

        // Đặt lịch kiểm tra auction sau durationMillis
        scheduler.schedule(() -> checkAndClose(auction), durationMillis, TimeUnit.MILLISECONDS);

        Response broadcastMsg = new Response(true, "AUCTION_UPDATED", auction);
        com.app.server.network.AuctionServer.broadcast(broadcastMsg);
    }


    private void loadAuctionsFromDatabase() {
        List<Auction> savedAuctions = auctionDao.getAllActiveAuctions();
        long currentTime = System.currentTimeMillis();

        for (Auction auction : savedAuctions) {
            activeAuctions.put(auction.getId(), auction);

            checkAndClose(auction);

//            long deltaWaitTime = auction.getEndTimeMillis() - currentTime;
//
//            if (deltaWaitTime > 0) {
//                scheduler.schedule(() -> checkAndClose(auction), deltaWaitTime, TimeUnit.MILLISECONDS);
//            } else {
//                // The auction expired while the server was offline -> conclude
//                concludeAuction(auction.getId());
//            }
        }
        logger.info("Loaded {} active auctions from the database.", activeAuctions.size());
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

        Auction finishedAuction = activeAuctions.remove(auctionId);

        if (finishedAuction != null) {
            finishedAuction.setStatus(Auction.Status.FINISHED);

            logger.info("Auction {} concluded - Winner: {}", auctionId, finishedAuction.getHighestBidderId());

            auctionDao.updateAuction(finishedAuction);
            BidService.getInstance().cleanupLock(auctionId);  // check cleanupLock()

            Response broadcastMsg = new Response(Response.ResponseType.AUCTION_UPDATED,true, "AUCTION_UPDATED", finishedAuction);
            com.app.server.network.AuctionServer.broadcast(broadcastMsg);
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

    public List<Auction> getAllActiveAuctionsList() {
        return new ArrayList<>(activeAuctions.values());
    }
}
