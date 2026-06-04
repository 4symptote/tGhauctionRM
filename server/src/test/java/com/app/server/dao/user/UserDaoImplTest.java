package com.app.server.dao.user;

import com.app.server.testutil.DatabaseConnectionMockHelper;
import com.app.server.dao.user.UserDao;
import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDaoImplTest {

    private DatabaseConnectionMockHelper databaseMock;
    private MongoCollection<Document> usersCollection;
    private FindIterable<Document> findIterable;
    private UserDao userDao;

    @BeforeEach
    void setUp() throws Exception {
        usersCollection = mock(MongoCollection.class);
        findIterable = mock(FindIterable.class);
        databaseMock = new DatabaseConnectionMockHelper();
        Class<?> daoClass = Class.forName("com.app.server.dao.user.UserDaoImpl");
        Method getInstance = daoClass.getMethod("getInstance");
        userDao = (UserDao) getInstance.invoke(null);
        ReflectionTestUtils.setField(userDao, "usersCollection", usersCollection);
    }

    @AfterEach
    void tearDown() {
        if (databaseMock != null) {
            databaseMock.close();
        }
    }

    @Test
    void saveAndUpdateUserShouldSerializeRoleSpecificBalances() {
        Bidder bidder = new Bidder("alice", "secret", "alice@example.com", 250.0, 40.0);
        bidder.setId("bidder-1");
        Seller seller = new Seller("seller", "secret", "seller@example.com", 500.0);
        seller.setId("seller-1");
        Admin admin = new Admin("admin", "secret", "admin@example.com");
        admin.setId("admin-1");

        userDao.saveUser(bidder);
        userDao.saveUser(seller);
        userDao.saveUser(admin);
        userDao.updateUser(bidder);

        var captor = org.mockito.ArgumentCaptor.forClass(Document.class);
        var replaceCaptor = org.mockito.ArgumentCaptor.forClass(Document.class);
        verify(usersCollection, times(3)).insertOne(captor.capture());
        assertThat(captor.getAllValues().get(0))
                .containsEntry("_id", "bidder-1")
                .containsEntry("role", "BIDDER")
                .containsEntry("balance", 250.0)
                .containsEntry("reservedBalance", 40.0);
        assertThat(captor.getAllValues().get(1))
                .containsEntry("_id", "seller-1")
                .containsEntry("role", "SELLER")
                .containsEntry("balance", 500.0)
                .containsEntry("reservedBalance", 0.0);
        assertThat(captor.getAllValues().get(2))
                .containsEntry("_id", "admin-1")
                .containsEntry("role", "ADMIN")
                .containsEntry("balance", 0.0)
                .containsEntry("reservedBalance", 0.0);
        verify(usersCollection).replaceOne(any(Document.class), replaceCaptor.capture());
        assertThat(replaceCaptor.getValue())
                .containsEntry("_id", "bidder-1")
                .containsEntry("role", "BIDDER");
    }

    @Test
    void getUserMethodsShouldMapDocumentsToAllRolesAndHandleMissingRecords() {
        Document adminDoc = userDoc("admin-1", "root", "hash", "root@example.com", "ADMIN", 0.0, null);
        Document sellerDoc = userDoc("seller-1", "seller", "hash", "seller@example.com", "SELLER", 120.5, null);
        Document bidderDoc = userDoc("bidder-1", "alice", "hash", "alice@example.com", "BIDDER", 800.0, null);

        when(usersCollection.find(any(Document.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(adminDoc, sellerDoc, bidderDoc, null);

        User admin = userDao.getUserByUsername("root");
        User seller = userDao.getUserByUsername("seller");
        User bidder = userDao.getUserById("bidder-1");
        User missing = userDao.getUserById("missing");

        assertThat(admin).isInstanceOf(Admin.class);
        assertThat(seller).isInstanceOf(Seller.class);
        assertThat(bidder).isInstanceOf(Bidder.class);
        assertThat(missing).isNull();
        assertThat(((Seller) seller).getTotalRevenue()).isEqualTo(120.5);
        assertThat(((Bidder) bidder).getBalance()).isEqualTo(800.0);
    }

    @Test
    void balanceMutationMethodsShouldRespectGuardClauses() {
        when(usersCollection.findOneAndUpdate(any(Document.class), any(Document.class)))
                .thenReturn(new Document("_id", "bidder-1"));

        assertThat(userDao.withdraw("bidder-1", 50.0)).isTrue();
        assertThat(userDao.withdraw("bidder-1", 0.0)).isFalse();
        assertThat(userDao.lockFunds("bidder-1", 75.0)).isTrue();
        assertThat(userDao.lockFunds("bidder-1", 0.0)).isFalse();

        userDao.deposit("bidder-1", 25.0);
        userDao.deposit("bidder-1", 0.0);
        userDao.adjustBalance("bidder-1", 10.0);
        userDao.unlockFunds("bidder-1", 25.0);
        userDao.unlockFunds("bidder-1", 0.0);

        verify(usersCollection, times(2)).findOneAndUpdate(any(Document.class), any(Document.class));
        verify(usersCollection, times(3)).updateOne(any(Document.class), any(Bson.class));
    }

    @Test
    void userExistsAndDeleteUserShouldDelegateToCollection() {
        DeleteResult deleteResult = mock(DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(1L);
        when(usersCollection.countDocuments(any(Document.class))).thenReturn(1L, 0L);
        when(usersCollection.deleteOne(any(Bson.class))).thenReturn(deleteResult);

        assertThat(userDao.userExists("alice")).isTrue();
        assertThat(userDao.userExists("missing")).isFalse();
        assertThat(userDao.deleteUser("alice")).isTrue();
    }

    private Document userDoc(String id, String username, String password, String email, String role,
                             double balance, Double reservedBalance) {
        Document doc = new Document("_id", id)
                .append("username", username)
                .append("passwordHash", password)
                .append("email", email)
                .append("role", role)
                .append("balance", balance);
        if (reservedBalance != null) {
            doc.append("reservedBalance", reservedBalance);
        }
        return doc;
    }
}
