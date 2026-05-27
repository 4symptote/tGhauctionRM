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
import com.app.server.dao.user.UserDao;
import com.app.server.dao.user.UserDaoImpl;
import com.app.shared.model.auction.Auction;

import com.app.shared.model.user.User;
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

    public void startAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
        auctionDao.saveAuction(auction);

        manageAuctionLifecycle(auction);

        com.app.server.network.AuctionServer.broadcast(new Response(
                Response.ResponseType.AUCTION_UPDATED,
                true, "AUCTION_STARTED",
                auction
        ));
    }


    private void loadAuctionsFromDatabase() {
        List<Auction> savedAuctions = auctionDao.getAllActiveAuctions();
        long currentTime = System.currentTimeMillis();

        for (Auction auction : savedAuctions) {
            activeAuctions.put(auction.getId(), auction);

            manageAuctionLifecycle(auction);
        }
        logger.info("Loaded {} active auctions from the database.", activeAuctions.size());
    }


    private void manageAuctionLifecycle(Auction auction) {
        // OPEN -> RUNNING -> FINISHED
        long currentTime = System.currentTimeMillis();
        long startTime = auction.getStartTime();
        long endTime = auction.getEndTimeMillis();

        if (auction.getStatus() == Auction.Status.PAID || auction.getStatus() == Auction.Status.CANCELED) {
            return;
        }

        if (currentTime < startTime) { // OPEN
            long delay = startTime - currentTime;
            scheduler.schedule(() -> {
                auction.updateStatus();
                com.app.server.network.AuctionServer.broadcast(new Response(
                        Response.ResponseType.AUCTION_UPDATED,
                        true, "AUCTION_STARTED",
                        auction
                ));

                manageAuctionLifecycle(auction);
            }, delay, TimeUnit.MILLISECONDS);

        } else if (currentTime < endTime) { // RUNNING
            long delay = endTime - currentTime;
            scheduler.schedule(() -> {
                auction.updateStatus();
                if (System.currentTimeMillis() < auction.getEndTimeMillis()) {
                    manageAuctionLifecycle(auction);
                } else {
                    concludeAuction(auction.getId());
                }
            }, delay, TimeUnit.MILLISECONDS);

        } else { // FINISHED
            concludeAuction(auction.getId());
        }
    }

    private void concludeAuction(String auctionId) {

        Auction finishedAuction = activeAuctions.remove(auctionId);

        //
        AutoBidService.getInstance().releaseAllEscrow(auctionId);

        if (finishedAuction != null) {
            finishedAuction.setStatus(Auction.Status.FINISHED);
            String winnerId = finishedAuction.getHighestBidderId();

            if (winnerId != null) {
                double winningPrice = finishedAuction.getCurrentPrice();
                String sellerId = finishedAuction.getSellerId();

                UserDao userDao = UserDaoImpl.getInstance();
                userDao.adjustBalance(sellerId, winningPrice);

                User updatedSeller = userDao.getUserById(sellerId);

                if (updatedSeller != null) {
                    com.app.server.network.AuctionServer.sendToClient(sellerId, new Response(
                            Response.ResponseType.USER_UPDATED,
                            true, "Item Sold. Revenue updated",
                            updatedSeller
                    ));
                }
            }
            logger.info("Auction {} concluded - Winner: {}", auctionId, finishedAuction.getHighestBidderId());

            auctionDao.updateAuction(finishedAuction);

            com.app.server.network.AuctionServer.broadcast(new Response(
                    Response.ResponseType.AUCTION_UPDATED,
                    true, "AUCTION_STARTED",
                    finishedAuction
            ));
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
