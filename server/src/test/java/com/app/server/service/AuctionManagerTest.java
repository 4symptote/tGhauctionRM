package com.app.server.service;

import com.app.server.dao.DatabaseConnection;
import com.app.server.dao.auction.AuctionDaoImpl;
import com.app.server.dao.user.UserDaoImpl;
import com.app.server.network.AuctionServer;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionManagerTest {

    private AuctionDaoImpl auctionDao;

    private UserDaoImpl userDao;

    @Mock
    private AutoBidService autoBidService;

    private AuctionManager auctionManager;
    private MockedStatic<DatabaseConnection> databaseConnectionStatic;

    @BeforeEach
    void setUp() throws Exception {
        databaseConnectionStatic = mockStatic(DatabaseConnection.class);
        DatabaseConnection connection = org.mockito.Mockito.mock(DatabaseConnection.class);
        MongoDatabase database = org.mockito.Mockito.mock(MongoDatabase.class);
        MongoCollection<Document> collection = org.mockito.Mockito.mock(MongoCollection.class);
        lenient().when(connection.getDatabase()).thenReturn(database);
        lenient().when(database.getCollection("auctions")).thenReturn(collection);
        lenient().when(database.getCollection("users")).thenReturn(collection);
        databaseConnectionStatic.when(DatabaseConnection::getInstance).thenReturn(connection);
        auctionDao = org.mockito.Mockito.mock(AuctionDaoImpl.class);
        userDao = org.mockito.Mockito.mock(UserDaoImpl.class);
        given(auctionDao.getAllActiveAuctions()).willReturn(List.of());
        try (MockedStatic<AuctionDaoImpl> mocked = mockStatic(AuctionDaoImpl.class)) {
            mocked.when(AuctionDaoImpl::getInstance).thenReturn(auctionDao);
            auctionManager = newManager();
        }
    }

    @AfterEach
    void tearDown() {
        if (databaseConnectionStatic != null) {
            databaseConnectionStatic.close();
        }
        shutdownScheduler();
        clearActiveAuctions();
    }

    @Test
    void startAuctionStoresAuctionAndBroadcastsUpdate() {
        Auction auction = runningAuction("auction-1", "seller-1", 100.0, 30);

        try (MockedStatic<AuctionServer> server = mockStatic(AuctionServer.class)) {
            auctionManager.startAuction(auction);

            assertThat(auctionManager.getAuction("auction-1")).isSameAs(auction);
            verify(auctionDao).saveAuction(auction);
            server.verify(() -> AuctionServer.broadcast(any()));
        }
    }

    @Test
    void startAuctionConcludesImmediatelyWhenAuctionAlreadyEnded() {
        Auction auction = finishedAuction("auction-2", "seller-1", 100.0);
        Seller seller = seller("seller-1", "seller");
        auction.setHighestBidderId("bidder-1");
        auction.setHighestBidderName("winner");

        given(userDao.getUserById("seller-1")).willReturn(seller);
        doNothing().when(userDao).adjustBalance("seller-1", 100.0);

        try (MockedStatic<UserDaoImpl> userDaoStatic = mockStatic(UserDaoImpl.class);
             MockedStatic<AutoBidService> autoBidStatic = mockStatic(AutoBidService.class);
             MockedStatic<AuctionServer> server = mockStatic(AuctionServer.class)) {

            userDaoStatic.when(UserDaoImpl::getInstance).thenReturn(userDao);
            AutoBidService autoBid = org.mockito.Mockito.mock(AutoBidService.class);
            autoBidStatic.when(AutoBidService::getInstance).thenReturn(autoBid);

            auctionManager.startAuction(auction);

            assertThat(auctionManager.getAuction("auction-2")).isNull();
            verify(auctionDao).saveAuction(auction);
            verify(auctionDao).updateAuction(auction);
            verify(userDao).adjustBalance("seller-1", 100.0);
            verify(userDao).getUserById("seller-1");
            server.verify(() -> AuctionServer.broadcast(any()), org.mockito.Mockito.times(2));
        }
    }

    @Test
    void forceDeleteAuctionRefundsWinnerAndDeletesAuction() {
        Auction auction = runningAuction("auction-3", "seller-1", 100.0, 30);
        auction.setHighestBidderId("bidder-1");
        auction.setHighestBidderName("winner");
        auction.setCurrentPrice(250.0);
        putActiveAuction(auction);

        Bidder winner = bidder("bidder-1", "winner", 100.0);
        given(userDao.getUserById("bidder-1")).willReturn(winner);
        doNothing().when(userDao).adjustBalance("bidder-1", 250.0);

        try (MockedStatic<UserDaoImpl> userDaoStatic = mockStatic(UserDaoImpl.class);
             MockedStatic<AutoBidService> autoBidStatic = mockStatic(AutoBidService.class);
             MockedStatic<AuctionServer> server = mockStatic(AuctionServer.class)) {

            userDaoStatic.when(UserDaoImpl::getInstance).thenReturn(userDao);
            AutoBidService autoBid = org.mockito.Mockito.mock(AutoBidService.class);
            autoBidStatic.when(AutoBidService::getInstance).thenReturn(autoBid);

            auctionManager.forceDeleteAuction("auction-3");

            assertThat(auctionManager.getAuction("auction-3")).isNull();
            verify(auctionDao).deleteAuction("auction-3");
            verify(userDao).adjustBalance("bidder-1", 250.0);
            verify(autoBid).releaseAllEscrow("auction-3");
            server.verify(() -> AuctionServer.sendToClient(eq("bidder-1"), any()));
        }
    }

    @Test
    void getAllActiveAuctionsListReturnsCopyOfInternalState() {
        Auction one = runningAuction("auction-10", "seller-1", 100.0, 30);
        Auction two = runningAuction("auction-11", "seller-1", 150.0, 30);
        putActiveAuction(one);
        putActiveAuction(two);

        List<Auction> copy = auctionManager.getAllActiveAuctionsList();
        assertThat(copy).containsExactlyInAnyOrder(one, two);

        copy.clear();
        assertThat(auctionManager.getAllActiveAuctionsList()).containsExactlyInAnyOrder(one, two);
    }

    private AuctionManager newManager() throws Exception {
        Constructor<AuctionManager> constructor = AuctionManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void shutdownScheduler() {
        try {
            Field schedulerField = AuctionManager.class.getDeclaredField("scheduler");
            schedulerField.setAccessible(true);
            java.util.concurrent.ScheduledExecutorService scheduler =
                    (java.util.concurrent.ScheduledExecutorService) schedulerField.get(auctionManager);
            scheduler.shutdownNow();
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private void clearActiveAuctions() {
        try {
            Field field = AuctionManager.class.getDeclaredField("activeAuctions");
            field.setAccessible(true);
            ((Map<String, Auction>) field.get(auctionManager)).clear();
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private void putActiveAuction(Auction auction) {
        try {
            Field field = AuctionManager.class.getDeclaredField("activeAuctions");
            field.setAccessible(true);
            ((Map<String, Auction>) field.get(auctionManager)).put(auction.getId(), auction);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Auction runningAuction(String id, String sellerId, double startingPrice, long durationMinutes) {
        Electronics item = new Electronics.Builder()
                .name("Camera")
                .desc("Test camera")
                .startingPrice(startingPrice)
                .sellerId(sellerId)
                .brand("Sony")
                .build();
        long now = System.currentTimeMillis();
        Auction auction = new Auction(item, now - 1_000L, now + TimeUnit.MINUTES.toMillis(durationMinutes));
        auction.setId(id);
        auction.setSellerId(sellerId);
        auction.setSellerName("Seller");
        auction.setCurrentPrice(startingPrice);
        auction.updateStatus();
        return auction;
    }

    private Auction finishedAuction(String id, String sellerId, double startingPrice) {
        Electronics item = new Electronics.Builder()
                .name("Camera")
                .desc("Test camera")
                .startingPrice(startingPrice)
                .sellerId(sellerId)
                .brand("Sony")
                .build();
        long now = System.currentTimeMillis();
        Auction auction = new Auction(item, now - TimeUnit.MINUTES.toMillis(10), now - 1_000L);
        auction.setId(id);
        auction.setSellerId(sellerId);
        auction.setSellerName("Seller");
        auction.setCurrentPrice(startingPrice);
        auction.updateStatus();
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
}
