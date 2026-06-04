package com.app.server.service;

import com.app.server.dao.DatabaseConnection;
import com.app.server.dao.user.UserDaoImpl;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoBidServiceTest {

    private UserDaoImpl userDao;

    private AutoBidService autoBidService;
    private MockedStatic<DatabaseConnection> databaseConnectionStatic;

    @BeforeEach
    void setUp() {
        databaseConnectionStatic = mockStatic(DatabaseConnection.class);
        DatabaseConnection connection = org.mockito.Mockito.mock(DatabaseConnection.class);
        com.mongodb.client.MongoDatabase database = org.mockito.Mockito.mock(com.mongodb.client.MongoDatabase.class);
        com.mongodb.client.MongoCollection<org.bson.Document> collection = org.mockito.Mockito.mock(com.mongodb.client.MongoCollection.class);
        lenient().when(connection.getDatabase()).thenReturn(database);
        lenient().when(database.getCollection("users")).thenReturn(collection);
        databaseConnectionStatic.when(DatabaseConnection::getInstance).thenReturn(connection);
        userDao = org.mockito.Mockito.mock(UserDaoImpl.class);
        autoBidService = AutoBidService.getInstance();
        clearState();
    }

    @AfterEach
    void tearDown() {
        if (databaseConnectionStatic != null) {
            databaseConnectionStatic.close();
        }
        clearState();
    }

    @Test
    void setOrUpdateAutoBid_registersNewEntryAndLocksFunds() {
        given(userDao.lockFunds("bidder-1", 200.0)).willReturn(true);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            autoBidService.setOrUpdateAutoBid("bidder-1", "auction-1", 200.0);

            assertThat(autoBidService.hasAutoBid("bidder-1", "auction-1")).isTrue();
            verify(userDao).lockFunds("bidder-1", 200.0);
        }
    }

    @Test
    void setOrUpdateAutoBid_updatesExistingEntryAndLocksAdditionalFunds() {
        given(userDao.lockFunds("bidder-1", 100.0)).willReturn(true);
        given(userDao.lockFunds("bidder-1", 50.0)).willReturn(true);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            autoBidService.setOrUpdateAutoBid("bidder-1", "auction-1", 100.0);
            autoBidService.setOrUpdateAutoBid("bidder-1", "auction-1", 150.0);

            verify(userDao).lockFunds("bidder-1", 100.0);
            verify(userDao).lockFunds("bidder-1", 50.0);
        }
    }

    @Test
    void setOrUpdateAutoBid_rejectsIncreaseWhenFundsCannotBeLocked() {
        given(userDao.lockFunds("bidder-1", 100.0)).willReturn(true);
        given(userDao.lockFunds("bidder-1", 50.0)).willReturn(false);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            autoBidService.setOrUpdateAutoBid("bidder-1", "auction-1", 100.0);

            assertThatThrownBy(() -> autoBidService.setOrUpdateAutoBid("bidder-1", "auction-1", 150.0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient balance");
        }
    }

    @Test
    void setOrUpdateAutoBid_rejectsLowerLimitWhenActiveBidsExceedLimit() throws Exception {
        given(userDao.lockFunds("bidder-1", 100.0)).willReturn(true);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            autoBidService.setOrUpdateAutoBid("bidder-1", "auction-1", 100.0);
            forceCurrentlyLocked("auction-1", "bidder-1", 10.0);

            assertThatThrownBy(() -> autoBidService.setOrUpdateAutoBid("bidder-1", "auction-1", 20.0))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot lower limit");
        }
    }

    @Test
    void evaluate_returnsEarlyWhenAuctionMissingOrNotRunning() {
        autoBidService.evaluate("missing-auction");

        Auction auction = runningAuction("auction-1", 100.0);
        auction.setStatus(Auction.Status.FINISHED);
        forcePutState("auction-1", "bidder-1", 120.0, 120.0);

        try (MockedStatic<AuctionManager> managerStatic = mockStatic(AuctionManager.class)) {
            AuctionManager auctionManager = org.mockito.Mockito.mock(AuctionManager.class);
            managerStatic.when(AuctionManager::getInstance).thenReturn(auctionManager);
            given(auctionManager.getAuction("auction-1")).willReturn(auction);

            autoBidService.evaluate("auction-1");
        }
    }

    @Test
    void releaseAllEscrow_unlocksAndRemovesAuctionState() {
        given(userDao.lockFunds("bidder-1", 100.0)).willReturn(true);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            autoBidService.setOrUpdateAutoBid("bidder-1", "auction-1", 100.0);
            autoBidService.releaseAllEscrow("auction-1");

            assertThat(autoBidService.hasAutoBid("bidder-1", "auction-1")).isFalse();
            verify(userDao).unlockFunds("bidder-1", 100.0);
        }
    }

    private void clearState() {
        try {
            Field autoBidLimitsField = AutoBidService.class.getDeclaredField("autoBidLimits");
            autoBidLimitsField.setAccessible(true);
            Map<?, ?> autoBidLimits = (Map<?, ?>) autoBidLimitsField.get(autoBidService);
            autoBidLimits.clear();

            Field engineLocksField = AutoBidService.class.getDeclaredField("engineLocks");
            engineLocksField.setAccessible(true);
            Map<?, ?> engineLocks = (Map<?, ?>) engineLocksField.get(autoBidService);
            engineLocks.clear();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void forceCurrentlyLocked(String auctionId, String userId, double value) throws Exception {
        Field autoBidLimitsField = AutoBidService.class.getDeclaredField("autoBidLimits");
        autoBidLimitsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> limits = (Map<String, Map<String, Object>>) autoBidLimitsField.get(autoBidService);
        Object state = limits.get(auctionId).get(userId);
        Field lockedField = state.getClass().getDeclaredField("currentlyLocked");
        lockedField.setAccessible(true);
        lockedField.setDouble(state, value);
    }

    private void forcePutState(String auctionId, String userId, double maxLimit, double currentlyLocked) {
        try {
            Field autoBidLimitsField = AutoBidService.class.getDeclaredField("autoBidLimits");
            autoBidLimitsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> limits = (Map<String, Map<String, Object>>) autoBidLimitsField.get(autoBidService);

            Object states = limits.computeIfAbsent(auctionId, key -> new java.util.concurrent.ConcurrentHashMap<>());
            Class<?> bidStateClass = Class.forName("com.app.server.service.AutoBidService$BidState");
            Constructor<?> ctor = bidStateClass.getDeclaredConstructor(double.class, double.class);
            ctor.setAccessible(true);
            Object state = ctor.newInstance(maxLimit, currentlyLocked);
            @SuppressWarnings("unchecked")
            Map<String, Object> stateMap = (Map<String, Object>) states;
            stateMap.put(userId, state);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Auction runningAuction(String id, double currentPrice) {
        Electronics item = new Electronics.Builder()
                .name("Phone")
                .desc("Test")
                .startingPrice(currentPrice)
                .sellerId("seller-1")
                .brand("Apple")
                .build();
        long now = System.currentTimeMillis();
        Auction auction = new Auction(item, now - 1_000L, now + TimeUnit.MINUTES.toMillis(30));
        auction.setId(id);
        auction.setSellerId("seller-1");
        auction.setSellerName("seller");
        auction.setCurrentPrice(currentPrice);
        auction.updateStatus();
        return auction;
    }

    private Bidder bidder(String id, String username, double balance) {
        Bidder bidder = new Bidder(username, "secret123", username + "@mail.test", balance, 0.0);
        bidder.setId(id);
        return bidder;
    }
}
