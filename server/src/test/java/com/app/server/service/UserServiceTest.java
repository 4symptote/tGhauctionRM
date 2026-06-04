package com.app.server.service;

import com.app.server.dao.DatabaseConnection;
import com.app.server.dao.user.UserDaoImpl;
import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.payload.LoginPayload;
import com.app.shared.network.payload.RegisterPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private UserDaoImpl userDao;

    private MockedStatic<DatabaseConnection> databaseConnectionStatic;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        databaseConnectionStatic = mockStatic(DatabaseConnection.class);
        DatabaseConnection connection = org.mockito.Mockito.mock(DatabaseConnection.class);
        com.mongodb.client.MongoDatabase database = org.mockito.Mockito.mock(com.mongodb.client.MongoDatabase.class);
        com.mongodb.client.MongoCollection<org.bson.Document> collection = org.mockito.Mockito.mock(com.mongodb.client.MongoCollection.class);
        lenient().when(connection.getDatabase()).thenReturn(database);
        lenient().when(database.getCollection("users")).thenReturn(collection);
        databaseConnectionStatic.when(DatabaseConnection::getInstance).thenReturn(connection);
        userDao = org.mockito.Mockito.mock(UserDaoImpl.class);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (databaseConnectionStatic != null) {
            databaseConnectionStatic.close();
        }
    }

    @Test
    void constructorSeedsAdminWhenMissing() throws Exception {
        given(userDao.userExists("admin")).willReturn(false);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            UserService service = newService();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userDao).saveUser(captor.capture());
            assertThat(service).isNotNull();
            assertThat(captor.getValue()).isInstanceOf(Admin.class);
            assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        }
    }

    @Test
    void loginReturnsUserWhenCredentialsAreValid() throws Exception {
        given(userDao.userExists("admin")).willReturn(true);
        String hash = BCrypt.hashpw("secret123", BCrypt.gensalt());
        Bidder stored = new Bidder("alice", hash, "alice@mail.test", 500.0, 0.0);
        stored.setId("user-1");
        given(userDao.getUserByUsername("alice")).willReturn(stored);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            UserService service = newService();
            User loggedIn = service.login(new LoginPayload("  ALICE  ", "secret123"));

            assertThat(loggedIn).isSameAs(stored);
        }
    }

    @Test
    void loginRejectsBlankCredentials() throws Exception {
        given(userDao.userExists("admin")).willReturn(true);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            UserService service = newService();

            assertThatThrownBy(() -> service.login(new LoginPayload(" ", "   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("username");
        }
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        given(userDao.userExists("admin")).willReturn(true);
        String hash = BCrypt.hashpw("secret123", BCrypt.gensalt());
        given(userDao.getUserByUsername("alice")).willReturn(new Bidder("alice", hash, "alice@mail.test", 500.0, 0.0));

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            UserService service = newService();

            assertThatThrownBy(() -> service.login(new LoginPayload("alice", "wrong-pass")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sai ten dang nhap");
        }
    }

    @Test
    void registerCreatesSellerAndPersistsUser() throws Exception {
        given(userDao.userExists("seller1")).willReturn(false);
        given(userDao.userExists("admin")).willReturn(true);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            UserService service = newService();
            User created = service.register(new RegisterPayload(" Seller1 ", "secret123", "seller@mail.test", "SELLER"));

            assertThat(created).isInstanceOf(Seller.class);
            assertThat(created.getUsername()).isEqualTo("seller1");
            verify(userDao).saveUser(org.mockito.ArgumentMatchers.any(User.class));
        }
    }

    @Test
    void registerRejectsShortUsername() throws Exception {
        given(userDao.userExists("admin")).willReturn(true);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            UserService service = newService();

            assertThatThrownBy(() -> service.register(new RegisterPayload("ab", "secret123", "a@mail.test", "BIDDER")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username");
        }
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        given(userDao.userExists("bob")).willReturn(true);
        given(userDao.userExists("admin")).willReturn(true);

        try (MockedStatic<UserDaoImpl> mocked = mockStatic(UserDaoImpl.class)) {
            mocked.when(UserDaoImpl::getInstance).thenReturn(userDao);

            UserService service = newService();

            assertThatThrownBy(() -> service.register(new RegisterPayload("bob", "secret123", "b@mail.test", "BIDDER")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exists");
        }
    }

    private UserService newService() throws Exception {
        Constructor<UserService> constructor = UserService.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
