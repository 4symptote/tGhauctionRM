package com.app.server.service;

import com.app.server.dao.auction.BidDao;
import com.app.server.dao.auction.BidDaoImpl;
import com.app.shared.exception.AuctionClosedException;
import com.app.shared.exception.AuctionNotFoundException;
import com.app.shared.exception.InvalidBidException;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.auction.BidTransaction;
import com.app.shared.model.user.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class BidService {
    private static final BidService instance = new BidService();
    private final AuctionManager auctionManager = AuctionManager.getInstance();
    private final BidDao bidDao = BidDaoImpl.getInstance();
    // Lock map (auctionId: lock)
    private final Map<String, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    // Anti-snipping settings
    private static final long ANTI_SNIPE_THRESHOLD = 60*1000 * 5;   // Trigger antisnipe nếu đặt bid khi thời gian còn dưới 5p
    private static final long EXTENSION_TIME = 60*1000 * 5;         // Thời gian thêm vào

    private BidService() {

    }

    public static BidService getInstance() {
        return instance;
    }

    // Khong dung synchronized, vi no se lock tat ca cac auction, nhieu nguoi bid cung luc nhieu auction se wait tung ng 1 mot
    // thay vi vay dung ReentrantLock, notice tao 1 lock cho moi auction tuc la chi synchronize trong 1 auction moi lock
    // ko phai nguyen cai ham (tat ca auction) (mac du app rac ko den muc nhieu ng bid cung luc nhu v de co the thay su khac biet)
    public Auction placeBid(String auctionId, User bidder, double amount) {
        // Theem lock vao auctionId nay neu chua co va lock()
        ReentrantLock auctionLock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
        auctionLock.lock();

        Auction auction = auctionManager.getAuction(auctionId);

        try {

            // Logic nghiep vu
            if (auction == null) {
                throw new AuctionNotFoundException("Auction không tồn tại.");
            }
            if (auction.getStatus() != Auction.Status.RUNNING) {
                throw new AuctionClosedException("Auction đã kết thúc.");
            }
            // Không thể tự Bid item mình sell (prob k bao h xảy ra trừ khi là admin mfa đã là admin thì phải lm j cx dc)
            if (auction.getSellerId().equals(bidder.getId())) {
                throw new InvalidBidException("Không thể tự Bid item của bản thân?!?.");
            }
            if (amount <= auction.getCurrentPrice()) {
                throw new InvalidBidException("Bid phải lớn hơn: " + auction.getCurrentPrice() + "$.");
            }
            // todo: check if bidder has enough money

            BidTransaction bid = new BidTransaction(auctionId, bidder.getId(), amount);
            bidDao.saveBid(bid);
            // Cập nhật giá + highest bidder
            auction.processNewBid(amount, bidder.getId());

            // - Anti-Snipping
            long timeLeft = auction.getEndTimeMillis() - System.currentTimeMillis();
            if (timeLeft < ANTI_SNIPE_THRESHOLD) {
                // thêm thời gian mới
                // Ví dụ Auction end lúc 15:30, đặt bid lúc 15:28, end time mới sẽ là 15:33 (Luôn có 5p cho các bidder khác react)
                auction.setEndTimeMillis(System.currentTimeMillis() + EXTENSION_TIME);
            }

            return auction;

        } finally { // Luon unlock neu co crash hay loi
            auctionLock.unlock();
        }
    }

    // chạy từ concludeAuction() cửa AuctionManager, xóa các entry không cần thiết nữa
    public void cleanupLock(String auctionId) {
        auctionLocks.remove(auctionId);
    }
}
