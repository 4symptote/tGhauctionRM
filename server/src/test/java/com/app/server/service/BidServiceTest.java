package com.app.server.service;

import com.app.server.dao.auction.AuctionDao;
import com.app.server.dao.auction.BidDao;
import com.app.server.dao.user.UserDao;
import com.app.server.network.AuctionServer;
import com.app.shared.exception.AuctionClosedException;
import com.app.shared.exception.AuctionNotFoundException;
import com.app.shared.exception.InvalidBidException;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.auction.BidTransaction;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidDao bidDao;

    @Mock
    private AuctionDao auctionDao;

    @Mock
    private UserDao userDao;

    @Mock
    private AuctionManager auctionManager;

    @Mock
    private AutoBidService autoBidService;

    private BidService bidService;

    @BeforeEach
    void setUp() {
        bidService = new BidService(bidDao, auctionDao, userDao, auctionManager, autoBidService);
    }

    @Test
    void placeBid_updatesAuctionAndSendsNotifications() {
        Auction auction = runningAuction("auction-1", "seller-1", 100.0, 30);
        Bidder bidder = bidder("bidder-1", "Alice", 1_000.0);

        given(auctionManager.getAuction("auction-1")).willReturn(auction);
        given(userDao.getUserById("bidder-1")).willReturn(bidder);

        try (MockedStatic<AuctionServer> auctionServer = mockStatic(AuctionServer.class)) {
            Auction updated = bidService.placeBid("auction-1", bidder, 150.0);

            assertThat(updated).isSameAs(auction);
            assertThat(updated.getCurrentPrice()).isEqualTo(150.0);
            assertThat(updated.getHighestBidderId()).isEqualTo("bidder-1");
            assertThat(updated.getHighestBidderName()).isEqualTo("Alice");
            assertThat(bidder.getBalance()).isEqualTo(850.0);

            ArgumentCaptor<BidTransaction> bidCaptor = ArgumentCaptor.forClass(BidTransaction.class);
            verify(bidDao).saveBid(bidCaptor.capture());
            assertThat(bidCaptor.getValue().auctionId()).isEqualTo("auction-1");
            assertThat(bidCaptor.getValue().bidderId()).isEqualTo("bidder-1");
            assertThat(bidCaptor.getValue().amount()).isEqualTo(150.0);

            verify(auctionDao).updateAuction(auction);
            verify(userDao).adjustBalance("bidder-1", -150.0);
            verify(autoBidService).evaluate("auction-1");
            auctionServer.verify(() -> AuctionServer.sendToClient(eq("bidder-1"), any()), times(1));
            auctionServer.verify(() -> AuctionServer.broadcast(any()), times(1));
        }
    }

    @Test
    void placeBid_throwsAuctionClosedExceptionWhenAuctionEnded() {
        Auction auction = finishedAuction("auction-2", "seller-1", 100.0);
        Bidder bidder = bidder("bidder-1", "Alice", 1_000.0);

        given(auctionManager.getAuction("auction-2")).willReturn(auction);

        assertThatThrownBy(() -> bidService.placeBid("auction-2", bidder, 150.0))
                .isInstanceOf(AuctionClosedException.class)
                .hasMessageContaining("Auction");

        verifyNoInteractions(userDao, bidDao, auctionDao);
        verify(autoBidService).evaluate("auction-2");
    }

    @Test
    void placeBid_throwsInvalidBidExceptionWhenAmountTooLow() {
        Auction auction = runningAuction("auction-3", "seller-1", 100.0, 30);
        Bidder bidder = bidder("bidder-1", "Alice", 1_000.0);

        given(auctionManager.getAuction("auction-3")).willReturn(auction);

        assertThatThrownBy(() -> bidService.placeBid("auction-3", bidder, 100.0))
                .isInstanceOf(InvalidBidException.class)
                .hasMessageContaining("phải lớn hơn");

        verifyNoInteractions(userDao, bidDao, auctionDao);
        verify(autoBidService).evaluate("auction-3");
    }

    @Test
    void placeBid_throwsInvalidBidExceptionWhenSellerBidsOwnAuction() {
        Auction auction = runningAuction("auction-4", "seller-1", 100.0, 30);
        User sellerBidder = bidder("seller-1", "Seller", 1_000.0);

        given(auctionManager.getAuction("auction-4")).willReturn(auction);

        assertThatThrownBy(() -> bidService.placeBid("auction-4", sellerBidder, 150.0))
                .isInstanceOf(InvalidBidException.class)
                .hasMessageContaining("tự Bid");

        verifyNoInteractions(userDao, bidDao, auctionDao);
        verify(autoBidService).evaluate("auction-4");
    }

    @Test
    void placeBid_runsOneSuccessfulBidUnderConcurrencyLock() throws Exception {
        Auction auction = runningAuction("auction-5", "seller-1", 100.0, 30);
        Bidder bidderOne = bidder("bidder-1", "Alice", 1_000.0);
        Bidder bidderTwo = bidder("bidder-2", "Bob", 1_000.0);

        given(auctionManager.getAuction("auction-5")).willReturn(auction);
        given(userDao.getUserById(anyString())).willAnswer(invocation -> switch (invocation.getArgument(0, String.class)) {
            case "bidder-1" -> bidderOne;
            case "bidder-2" -> bidderTwo;
            case "seller-1" -> seller("seller-1", "Seller");
            default -> null;
        });

        try (MockedStatic<AuctionServer> auctionServer = mockStatic(AuctionServer.class)) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Future<Throwable> first = executor.submit(() -> runConcurrentBid(ready, start, () -> bidService.placeBid("auction-5", bidderOne, 200.0)));
            Future<Throwable> second = executor.submit(() -> runConcurrentBid(ready, start, () -> bidService.placeBid("auction-5", bidderTwo, 200.0)));

            assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Throwable firstError = first.get(3, TimeUnit.SECONDS);
            Throwable secondError = second.get(3, TimeUnit.SECONDS);

            executor.shutdownNow();

            assertThat(auction.getCurrentPrice()).isEqualTo(200.0);
            assertThat(auction.getHighestBidderId()).isIn("bidder-1", "bidder-2");
            assertThat((firstError == null) ^ (secondError == null)).isTrue();
            assertThat(firstError == null ? secondError : firstError).isInstanceOf(InvalidBidException.class);

            verify(auctionDao, times(1)).updateAuction(auction);
            verify(bidDao, times(1)).saveBid(any(BidTransaction.class));
        }
    }

    private Throwable runConcurrentBid(CountDownLatch ready, CountDownLatch start, BidAction action) {
        try {
            ready.countDown();
            start.await(2, TimeUnit.SECONDS);
            action.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private Auction runningAuction(String id, String sellerId, double startingPrice, long durationMinutes) {
        Auction auction = baseAuction(id, sellerId, startingPrice, durationMinutes);
        auction.updateStatus();
        return auction;
    }

    private Auction finishedAuction(String id, String sellerId, double startingPrice) {
        long now = System.currentTimeMillis();
        Auction auction = baseAuction(id, sellerId, startingPrice, -1);
        auction.setStartTime(now - 10_000L);
        auction.setEndTimeMillis(now - 5_000L);
        auction.updateStatus();
        return auction;
    }

    private Auction baseAuction(String id, String sellerId, double startingPrice, long durationMinutes) {
        Electronics item = new Electronics.Builder()
                .name("Laptop")
                .desc("Test laptop")
                .startingPrice(startingPrice)
                .sellerId(sellerId)
                .brand("Dell")
                .build();
        long now = System.currentTimeMillis();
        long start = now - 5_000L;
        long end = durationMinutes > 0 ? now + TimeUnit.MINUTES.toMillis(durationMinutes) : now - 1_000L;
        Auction auction = new Auction(item, start, end);
        auction.setId(id);
        auction.setSellerId(sellerId);
        auction.setSellerName("Seller");
        auction.setCurrentPrice(startingPrice);
        return auction;
    }

    private Bidder bidder(String id, String username, double balance) {
        Bidder bidder = new Bidder(username, "secret123", username + "@mail.test", balance, 0.0);
        bidder.setId(id);
        return bidder;
    }

    private Seller seller(String id, String username) {
        Seller seller = new Seller(username, "secret123", username + "@mail.test", 0.0);
        seller.setId(id);
        return seller;
    }

    @FunctionalInterface
    private interface BidAction {
        void run();
    }
}
