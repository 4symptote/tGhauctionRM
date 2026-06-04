package com.app.server.network.handler;

import com.app.server.dao.auction.AuctionDaoImpl;
import com.app.server.dao.auction.BidDaoImpl;
import com.app.server.dao.user.UserDaoImpl;
import com.app.server.dao.DatabaseConnection;
import com.app.server.network.AuctionServer;
import com.app.server.network.ClientHandler;
import com.app.server.service.AuctionManager;
import com.app.server.service.AutoBidService;
import com.app.server.service.BidService;
import com.app.server.service.UserService;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import com.app.shared.network.payload.AutoBidPayload;
import com.app.shared.network.payload.BidPayload;
import com.app.shared.network.payload.LoginPayload;
import com.app.shared.network.payload.RegisterPayload;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerHandlersTest {

    private MockedStatic<DatabaseConnection> databaseConnectionStatic;

    @BeforeEach
    void setUp() {
        databaseConnectionStatic = mockStatic(DatabaseConnection.class);
        DatabaseConnection connection = mock(DatabaseConnection.class);
        MongoDatabase database = mock(MongoDatabase.class);
        MongoCollection<Document> collection = mock(MongoCollection.class);

        lenient().when(connection.getDatabase()).thenReturn(database);
        lenient().when(database.getCollection("auctions")).thenReturn(collection);
        lenient().when(database.getCollection("bids")).thenReturn(collection);
        lenient().when(database.getCollection("users")).thenReturn(collection);

        databaseConnectionStatic.when(DatabaseConnection::getInstance).thenReturn(connection);
    }

    @AfterEach
    void tearDown() {
        if (databaseConnectionStatic != null) {
            databaseConnectionStatic.close();
        }
    }

    @Test
    void loginHandlerAuthenticatesAndSetsCurrentUser() {
        ClientHandler client = mock(ClientHandler.class);
        UserService userService = mock(UserService.class);
        User authenticatedUser = bidder("bidder-1", "alice", 500.0);
        when(client.getCurrentUser()).thenReturn(null);
        when(userService.login(new LoginPayload("alice", "secret123"))).thenReturn(authenticatedUser);

        try (var mocked = mockStatic(UserService.class)) {
            mocked.when(UserService::getInstance).thenReturn(userService);

            Response response = new LoginHandler().handle(new Request(Request.RequestType.LOGIN, new LoginPayload("alice", "secret123")), client);

            assertThat(response.success()).isTrue();
            assertThat(response.payload()).isSameAs(authenticatedUser);
            verify(client).setCurrentUser(authenticatedUser);
        }
    }

    @Test
    void loginHandlerRejectsDoubleLogin() {
        ClientHandler client = mock(ClientHandler.class);
        when(client.getCurrentUser()).thenReturn(bidder("bidder-1", "alice", 500.0));

        Response response = new LoginHandler().handle(new Request(Request.RequestType.LOGIN, new LoginPayload("alice", "secret123")), client);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("chill out");
    }

    @Test
    void registerHandlerRegistersUserAndSetsCurrentUser() {
        ClientHandler client = mock(ClientHandler.class);
        UserService userService = mock(UserService.class);
        User newUser = seller("seller-1", "seller");
        when(client.getCurrentUser()).thenReturn(null);
        when(userService.register(new RegisterPayload("seller", "secret123", "seller@example.com", "SELLER"))).thenReturn(newUser);

        try (var mocked = mockStatic(UserService.class)) {
            mocked.when(UserService::getInstance).thenReturn(userService);

            Response response = new RegisterHandler().handle(
                    new Request(Request.RequestType.REGISTER, new RegisterPayload("seller", "secret123", "seller@example.com", "SELLER")),
                    client);

            assertThat(response.success()).isTrue();
            assertThat(response.payload()).isSameAs(newUser);
            verify(client).setCurrentUser(newUser);
        }
    }

    @Test
    void placeBidHandlerDelegatesToBidServiceAndUpdatesAuction() {
        ClientHandler client = mock(ClientHandler.class);
        BidService bidService = mock(BidService.class);
        AuctionDaoImpl auctionDao = mock(AuctionDaoImpl.class);
        Auction auction = runningAuction("auction-1", "seller-1", 100.0, 30);
        Bidder bidder = bidder("bidder-1", "alice", 500.0);
        when(client.getCurrentUser()).thenReturn(bidder);
        when(bidService.placeBid("auction-1", bidder, 150.0)).thenReturn(auction);

        try (var bidServiceStatic = mockStatic(BidService.class);
             var auctionDaoStatic = mockStatic(AuctionDaoImpl.class)) {
            bidServiceStatic.when(BidService::getInstance).thenReturn(bidService);
            auctionDaoStatic.when(AuctionDaoImpl::getInstance).thenReturn(auctionDao);

            Response response = new PlaceBidHandler().handle(
                    new Request(Request.RequestType.PLACE_BID, new BidPayload("auction-1", 150.0)),
                    client);

            assertThat(response.success()).isTrue();
            assertThat(response.type()).isEqualTo(Response.ResponseType.PLACED_BID);
            verify(auctionDao).updateAuction(auction);
        }
    }

    @Test
    void placeBidHandlerRejectsNonBidders() {
        ClientHandler client = mock(ClientHandler.class);
        when(client.getCurrentUser()).thenReturn(seller("seller-1", "seller"));

        Response response = new PlaceBidHandler().handle(
                new Request(Request.RequestType.PLACE_BID, new BidPayload("auction-1", 150.0)),
                client);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Only bidder can bid on items");
    }

    @Test
    void getAuctionsHandlerReturnsAuctionList() {
        ClientHandler client = mock(ClientHandler.class);
        AuctionManager auctionManager = mock(AuctionManager.class);
        List<Auction> auctions = List.of(runningAuction("auction-1", "seller-1", 100.0, 30));
        when(auctionManager.getAllActiveAuctionsList()).thenReturn(auctions);

        try (var mocked = mockStatic(AuctionManager.class)) {
            mocked.when(AuctionManager::getInstance).thenReturn(auctionManager);

            Response response = new GetAuctionsHandler().handle(new Request(Request.RequestType.GET_AUCTIONS, null), client);

            assertThat(response.success()).isTrue();
            assertThat(response.type()).isEqualTo(Response.ResponseType.AUCTION_LIST);
            assertThat(response.payload()).isSameAs(auctions);
        }
    }

    @Test
    void getBidHistoryHandlerReturnsBidHistory() {
        ClientHandler client = mock(ClientHandler.class);
        BidDaoImpl bidDao = mock(BidDaoImpl.class);
        List<com.app.shared.model.auction.BidTransaction> history = List.of(
                new com.app.shared.model.auction.BidTransaction("auction-1", "bidder-1", "alice", 123.0));
        when(bidDao.getBidsForAuction("auction-1")).thenReturn(history);

        try (var mocked = mockStatic(BidDaoImpl.class)) {
            mocked.when(BidDaoImpl::getInstance).thenReturn(bidDao);

            Response response = new GetBidHistoryHandler().handle(new Request(Request.RequestType.GET_BID_HISTORY, "auction-1"), client);

            assertThat(response.success()).isTrue();
            assertThat(response.type()).isEqualTo(Response.ResponseType.BID_HISTORY);
            assertThat(response.payload()).isSameAs(history);
        }
    }

    @Test
    void getSellerAuctionsHandlerRequiresSellerAndUpdatesStatuses() {
        ClientHandler client = mock(ClientHandler.class);
        when(client.getCurrentUser()).thenReturn(seller("seller-1", "seller"));
        AuctionDaoImpl auctionDao = mock(AuctionDaoImpl.class);
        Auction auction = runningAuction("auction-1", "seller-1", 100.0, 30);
        when(auctionDao.getAuctionsBySellerId("seller-1")).thenReturn(List.of(auction));

        try (var mocked = mockStatic(AuctionDaoImpl.class)) {
            mocked.when(AuctionDaoImpl::getInstance).thenReturn(auctionDao);

            Response response = new GetSellerAuctionsHandler().handle(new Request(Request.RequestType.GET_SELLER_AUCTIONS, null), client);

            assertThat(response.success()).isTrue();
            assertThat(response.type()).isEqualTo(Response.ResponseType.SELLER_AUCTION_LIST);
            assertThat((List<Auction>) response.payload()).containsExactly(auction);
        }
    }

    @Test
    void getWinningAuctionsHandlerRequiresBidder() {
        ClientHandler client = mock(ClientHandler.class);
        when(client.getCurrentUser()).thenReturn(bidder("bidder-1", "alice", 500.0));
        AuctionDaoImpl auctionDao = mock(AuctionDaoImpl.class);
        List<Auction> auctions = List.of(runningAuction("auction-1", "seller-1", 100.0, 30));
        when(auctionDao.getAuctionsByHighestBidderId("bidder-1")).thenReturn(auctions);

        try (var mocked = mockStatic(AuctionDaoImpl.class)) {
            mocked.when(AuctionDaoImpl::getInstance).thenReturn(auctionDao);

            Response response = new GetWinningAuctionsHandler().handle(new Request(Request.RequestType.GET_WINNING_AUCTIONS, null), client);

            assertThat(response.success()).isTrue();
            assertThat(response.type()).isEqualTo(Response.ResponseType.WINNING_AUCTION_LIST);
            assertThat(response.payload()).isSameAs(auctions);
        }
    }

    @Test
    void logoutHandlerClearsCurrentUser() {
        ClientHandler client = mock(ClientHandler.class);
        when(client.getCurrentUser()).thenReturn(bidder("bidder-1", "alice", 500.0));

        Response response = new LogoutHandler().handle(new Request(Request.RequestType.LOGOUT, null), client);

        assertThat(response.success()).isTrue();
        verify(client).setCurrentUser(null);
    }

    @Test
    void depositHandlerUpdatesUserBalance() {
        ClientHandler client = mock(ClientHandler.class);
        UserDaoImpl userDao = mock(UserDaoImpl.class);
        Bidder bidder = bidder("bidder-1", "alice", 500.0);
        Bidder updated = bidder("bidder-1", "alice", 750.0);
        when(client.getCurrentUser()).thenReturn(bidder);
        when(userDao.getUserById("bidder-1")).thenReturn(updated);

        try (var mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            Response response = new DepositHandler().handle(new Request(Request.RequestType.DEPOSIT, 250.0), client);

            assertThat(response.success()).isTrue();
            assertThat(response.type()).isEqualTo(Response.ResponseType.USER_UPDATED);
            verify(client).setCurrentUser(updated);
        }
    }

    @Test
    void withdrawHandlerSupportsSuccessAndFailure() {
        ClientHandler client = mock(ClientHandler.class);
        UserDaoImpl userDao = mock(UserDaoImpl.class);
        Seller seller = seller("seller-1", "seller");
        Seller updated = seller("seller-1", "seller");
        updated.collectRevenue(200.0);
        when(client.getCurrentUser()).thenReturn(seller);
        when(userDao.withdraw("seller-1", 200.0)).thenReturn(true);
        when(userDao.getUserById("seller-1")).thenReturn(updated);

        try (var mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            Response success = new WithdrawHandler().handle(new Request(Request.RequestType.WITHDRAW, 200.0), client);
            assertThat(success.success()).isTrue();
            verify(client).setCurrentUser(updated);
        }

        when(userDao.withdraw("seller-1", 500.0)).thenReturn(false);
        try (var mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            Response failure = new WithdrawHandler().handle(new Request(Request.RequestType.WITHDRAW, 500.0), client);
            assertThat(failure.success()).isFalse();
        }
    }

    @Test
    void setAutoBidHandlerSetsAutoBidAndRefreshesUser() {
        ClientHandler client = mock(ClientHandler.class);
        UserDaoImpl userDao = mock(UserDaoImpl.class);
        AutoBidService autoBidService = mock(AutoBidService.class);
        Bidder bidder = bidder("bidder-1", "alice", 500.0);
        Bidder updated = bidder("bidder-1", "alice", 300.0);
        when(client.getCurrentUser()).thenReturn(bidder);
        when(autoBidService.hasAutoBid("bidder-1", "auction-1")).thenReturn(false);
        when(userDao.getUserById("bidder-1")).thenReturn(updated);

        try (var autoBidStatic = mockStatic(AutoBidService.class);
             var userDaoStatic = mockStatic(UserDaoImpl.class)) {
            autoBidStatic.when(AutoBidService::getInstance).thenReturn(autoBidService);
            userDaoStatic.when(UserDaoImpl::getInstance).thenReturn(userDao);

            Response response = new SetAutoBidHandler().handle(new Request(Request.RequestType.SET_AUTO_BID, new AutoBidPayload("auction-1", 200.0)), client);

            assertThat(response.success()).isTrue();
            assertThat(response.type()).isEqualTo(Response.ResponseType.AUTO_BID_SET);
            verify(autoBidService).setOrUpdateAutoBid("bidder-1", "auction-1", 200.0);
            verify(client).setCurrentUser(updated);
        }
    }

    @Test
    void adminDeleteHandlersRequireAdmin() {
        ClientHandler client = mock(ClientHandler.class);
        when(client.getCurrentUser()).thenReturn(seller("seller-1", "seller"));

        Response auctionResponse = new AdminDeleteAuctionHandler().handle(new Request(Request.RequestType.ADMIN_DELETE_AUCTION, "auction-1"), client);
        Response userResponse = new AdminDeleteUserHandler().handle(new Request(Request.RequestType.ADMIN_DELETE_USER, "user-1"), client);

        assertThat(auctionResponse.success()).isFalse();
        assertThat(userResponse.success()).isFalse();
    }

    @Test
    void adminDeleteHandlersCallAdminActions() {
        ClientHandler client = mock(ClientHandler.class);
        when(client.getCurrentUser()).thenReturn(new Admin("admin", "hash", "admin@example.com"));
        AuctionManager auctionManager = mock(AuctionManager.class);
        UserDaoImpl userDao = mock(UserDaoImpl.class);

        try (var auctionManagerStatic = mockStatic(AuctionManager.class);
             var userDaoStatic = mockStatic(UserDaoImpl.class);
             var auctionServerStatic = mockStatic(AuctionServer.class)) {
            auctionManagerStatic.when(AuctionManager::getInstance).thenReturn(auctionManager);
            userDaoStatic.when(UserDaoImpl::getInstance).thenReturn(userDao);
            when(userDao.deleteUser("user-1")).thenReturn(true);

            Response auctionResponse = new AdminDeleteAuctionHandler().handle(new Request(Request.RequestType.ADMIN_DELETE_AUCTION, "auction-1"), client);
            Response userResponse = new AdminDeleteUserHandler().handle(new Request(Request.RequestType.ADMIN_DELETE_USER, "user-1"), client);

            assertThat(auctionResponse.success()).isTrue();
            assertThat(userResponse.success()).isTrue();
            verify(auctionManager).forceDeleteAuction("auction-1");
            verify(userDao).deleteUser("user-1");
            auctionServerStatic.verify(() -> AuctionServer.broadcast(any()));
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
