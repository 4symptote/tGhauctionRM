package com.app.server.dao.auction;

import com.app.server.testutil.DatabaseConnectionMockHelper;
import com.app.server.dao.auction.BidDao;
import com.app.shared.model.auction.BidTransaction;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BidDaoImplTest {

    private DatabaseConnectionMockHelper databaseMock;
    private MongoCollection<Document> collection;
    private BidDao bidDao;

    @BeforeEach
    void setUp() throws Exception {
        collection = mock(MongoCollection.class);
        databaseMock = new DatabaseConnectionMockHelper();
        Class<?> daoClass = Class.forName("com.app.server.dao.auction.BidDaoImpl");
        Method getInstance = daoClass.getMethod("getInstance");
        bidDao = (BidDao) getInstance.invoke(null);
        ReflectionTestUtils.setField(bidDao, "collection", collection);
    }

    @AfterEach
    void tearDown() {
        if (databaseMock != null) {
            databaseMock.close();
        }
    }

    @Test
    void saveBidShouldInsertExpectedDocument() {
        BidTransaction bid = new BidTransaction("auction-1", "bidder-1", "Alice", 123.45);

        bidDao.saveBid(bid);

        var captor = org.mockito.ArgumentCaptor.forClass(Document.class);
        verify(collection).insertOne(captor.capture());
        assertThat(captor.getValue())
                .containsEntry("_id", bid.id())
                .containsEntry("auctionId", "auction-1")
                .containsEntry("bidderId", "bidder-1")
                .containsEntry("bidderName", "Alice")
                .containsEntry("amount", 123.45);
    }

    @Test
    void getBidsForAuctionShouldReturnSortedHistoryAndDefaultMissingBidderName() {
        Document first = bidDocument("bid-1", "auction-1", "bidder-1", null, 200.0, 2L);
        Document second = bidDocument("bid-2", "auction-1", "bidder-2", "Bob", 300.0, 3L);
        FindIterable<Document> iterable = findIterable(List.of(first, second));
        when(collection.find(any(Bson.class))).thenReturn(iterable);

        List<BidTransaction> history = bidDao.getBidsForAuction("auction-1");

        assertThat(history).hasSize(2);
        assertThat(history.get(0).bidderName()).isEqualTo("Unknown");
        assertThat(history.get(1).bidderName()).isEqualTo("Bob");
        assertThat(history.get(0).amount()).isEqualTo(200.0);
    }

    @Test
    void getBidsByUserShouldMapUserHistory() {
        Document doc = bidDocument("bid-3", "auction-2", "bidder-9", "Charlie", 999.0, 5L);
        when(collection.find(any(Bson.class))).thenReturn(findIterable(List.of(doc)));

        List<BidTransaction> history = bidDao.getBidsByUser("bidder-9");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).id()).isEqualTo("bid-3");
        assertThat(history.get(0).auctionId()).isEqualTo("auction-2");
        assertThat(history.get(0).bidderName()).isEqualTo("Charlie");
    }

    private Document bidDocument(String id, String auctionId, String bidderId, String bidderName, double amount, long timestamp) {
        Document doc = new Document("_id", id)
                .append("auctionId", auctionId)
                .append("bidderId", bidderId)
                .append("amount", amount)
                .append("timestamp", timestamp);
        if (bidderName != null) {
            doc.append("bidderName", bidderName);
        }
        return doc;
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
}
