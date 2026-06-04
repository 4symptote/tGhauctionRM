package com.app.server.dao.auction;

import com.app.server.testutil.DatabaseConnectionMockHelper;
import com.app.server.dao.auction.AuctionDao;
import com.app.server.dao.user.UserDaoImpl;
import com.app.shared.model.auction.Auction;
import com.app.shared.model.item.Electronics;
import com.app.shared.model.user.Seller;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuctionDaoImplTest {

    private DatabaseConnectionMockHelper databaseMock;
    private MongoCollection<Document> collection;
    private AuctionDao auctionDao;

    @BeforeEach
    void setUp() throws Exception {
        collection = mock(MongoCollection.class);
        databaseMock = new DatabaseConnectionMockHelper();
        Class<?> daoClass = Class.forName("com.app.server.dao.auction.AuctionDaoImpl");
        Method getInstance = daoClass.getMethod("getInstance");
        auctionDao = (AuctionDao) getInstance.invoke(null);
        ReflectionTestUtils.setField(auctionDao, "collection", collection);
    }

    @AfterEach
    void tearDown() {
        if (databaseMock != null) {
            databaseMock.close();
        }
    }

    @Test
    void saveAndUpdateAuctionShouldSerializeAuctionToDocuments() {
        Auction auction = runningAuction("auction-1", "seller-1", "Seller", 100.0);

        auctionDao.saveAuction(auction);
        auctionDao.updateAuction(auction);

        var insertCaptor = org.mockito.ArgumentCaptor.forClass(Document.class);
        var replaceCaptor = org.mockito.ArgumentCaptor.forClass(Document.class);
        var optionsCaptor = org.mockito.ArgumentCaptor.forClass(com.mongodb.client.model.ReplaceOptions.class);
        verify(collection).insertOne(insertCaptor.capture());
        verify(collection).replaceOne(any(Bson.class), replaceCaptor.capture(), optionsCaptor.capture());

        assertThat(insertCaptor.getValue())
                .containsEntry("_id", "auction-1")
                .containsEntry("sellerId", "seller-1")
                .containsEntry("sellerName", "Seller")
                .containsEntry("status", "RUNNING");
        assertThat(((Document) insertCaptor.getValue().get("item")).getString("type")).isEqualTo("Electronics");
        assertThat(replaceCaptor.getValue()).containsEntry("_id", "auction-1");
        assertThat(optionsCaptor.getValue().isUpsert()).isTrue();
    }

    @Test
    void getAuctionsBySellerIdShouldMapSellerNameAndItemData() {
        Document auctionDoc = auctionDocument("auction-1", "seller-1", "bidder-1", "RUNNING", "seller-1");
        when(collection.find(any(Bson.class))).thenReturn(findIterable(List.of(auctionDoc)));

        UserDaoImpl userDao = mock(UserDaoImpl.class);
        when(userDao.getUserById("seller-1")).thenReturn(seller("seller-1", "seller"));

        try (var mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            List<Auction> auctions = auctionDao.getAuctionsBySellerId("seller-1");

            assertThat(auctions).hasSize(1);
            assertThat(auctions.get(0).getSellerName()).isEqualTo("seller");
            assertThat(auctions.get(0).getHighestBidderId()).isEqualTo("bidder-1");
            assertThat(auctions.get(0).getItem()).isInstanceOf(Electronics.class);
        }
    }

    @Test
    void getAuctionsByHighestBidderIdShouldMapMatchedAuctions() {
        Document auctionDoc = auctionDocument("auction-2", "seller-1", "bidder-2", "FINISHED", "seller-1");
        when(collection.find(any(Bson.class))).thenReturn(findIterable(List.of(auctionDoc)));

        UserDaoImpl userDao = mock(UserDaoImpl.class);
        when(userDao.getUserById("seller-1")).thenReturn(seller("seller-1", "seller"));

        try (var mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            List<Auction> auctions = auctionDao.getAuctionsByHighestBidderId("bidder-2");

            assertThat(auctions).hasSize(1);
            assertThat(auctions.get(0).getId()).isEqualTo("auction-2");
            assertThat(auctions.get(0).getStatus()).isEqualTo(Auction.Status.FINISHED);
        }
    }

    @Test
    void getAllActiveAuctionsShouldFallBackToUnknownSellerNameWhenUsernameMissing() {
        Document auctionDoc = auctionDocument("auction-3", "seller-2", null, "OPEN", "seller-2");
        when(collection.find(any(Bson.class))).thenReturn(findIterable(List.of(auctionDoc)));

        UserDaoImpl userDao = mock(UserDaoImpl.class);
        when(userDao.getUserById("seller-2")).thenReturn(seller("seller-2", null));

        try (var mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            List<Auction> auctions = auctionDao.getAllActiveAuctions();

            assertThat(auctions).hasSize(1);
            assertThat(auctions.get(0).getSellerName()).isEqualTo("Unknown");
            assertThat(auctions.get(0).getStatus()).isEqualTo(Auction.Status.OPEN);
        }
    }

    @Test
    void deleteAuctionShouldDelegateToCollection() {
        auctionDao.deleteAuction("auction-9");

        verify(collection).deleteOne(any(Bson.class));
    }

    private Auction runningAuction(String id, String sellerId, String sellerName, double startingPrice) {
        Electronics item = new Electronics.Builder()
                .name("Camera")
                .desc("Test camera")
                .startingPrice(startingPrice)
                .sellerId(sellerId)
                .brand("Sony")
                .build();
        long now = System.currentTimeMillis();
        Auction auction = new Auction(item, now - 1_000L, now + 60_000L);
        auction.setId(id);
        auction.setSellerId(sellerId);
        auction.setSellerName(sellerName);
        auction.setCurrentPrice(startingPrice + 50.0);
        auction.setHighestBidderId("bidder-1");
        auction.setHighestBidderName("Alice");
        auction.setStatus(Auction.Status.RUNNING);
        return auction;
    }

    private Document auctionDocument(String id, String sellerId, String highestBidderId, String status, String sellerName) {
        Electronics item = new Electronics.Builder()
                .name("Camera")
                .desc("Test camera")
                .startingPrice(100.0)
                .sellerId(sellerId)
                .brand("Sony")
                .build();
        return new Document("_id", id)
                .append("sellerName", sellerName)
                .append("sellerId", sellerId)
                .append("item", item.toBsonDocument())
                .append("currentPrice", 150.0)
                .append("highestBidderId", highestBidderId)
                .append("highestBidderName", "Alice")
                .append("status", status)
                .append("startTime", System.currentTimeMillis() - 1_000L)
                .append("endTime", System.currentTimeMillis() + 60_000L);
    }

    private FindIterable<Document> findIterable(List<Document> documents) {
        class ListCursor implements com.mongodb.client.MongoCursor<Document> {
            private int index;

            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return index < documents.size();
            }

            @Override
            public Document next() {
                return documents.get(index++);
            }

            @Override
            public int available() {
                return documents.size() - index;
            }

            @Override
            public Document tryNext() {
                return hasNext() ? next() : null;
            }

            @Override
            public com.mongodb.ServerCursor getServerCursor() {
                return null;
            }

            @Override
            public com.mongodb.ServerAddress getServerAddress() {
                return null;
            }
        }

        ListCursor cursor = new ListCursor();
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "iterator", "cursor" -> cursor;
            case "first" -> documents.isEmpty() ? null : documents.get(0);
            case "sort", "filter", "limit", "skip", "maxTime", "maxAwaitTime", "projection", "noCursorTimeout",
                 "partial", "cursorType", "batchSize", "collation", "comment", "hint", "hintString", "let",
                 "max", "min", "returnKey", "showRecordId", "allowDiskUse", "timeoutMode" -> proxy;
            case "into" -> {
                @SuppressWarnings("unchecked")
                java.util.Collection<Document> target = (java.util.Collection<Document>) args[0];
                target.addAll(documents);
                yield target;
            }
            default -> method.getReturnType().isPrimitive() ? defaultPrimitive(method.getReturnType()) : null;
        };

        @SuppressWarnings("unchecked")
        FindIterable<Document> iterable = (FindIterable<Document>) Proxy.newProxyInstance(
                FindIterable.class.getClassLoader(),
                new Class<?>[]{FindIterable.class},
                handler);
        return iterable;
    }

    private Object defaultPrimitive(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0d;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }

    private Seller seller(String id, String username) {
        Seller seller = new Seller(username, "secret123", "seller@example.com", 0.0);
        seller.setId(id);
        return seller;
    }
}
