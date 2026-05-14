package com.app.server.service;

import com.app.server.dao.user.UserDao;
import com.app.server.dao.user.UserDaoImpl;
import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.payload.LoginPayload;
import com.app.shared.network.payload.RegisterPayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static UserService instance;

    // temp db
    private final Map<String, User> tempDatabase = new HashMap<>();

    private final UserDao userDao = new UserDaoImpl();

    private UserService() {
        //todo: prob seed admin
        seedAdmin();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    // Synchronized boi vi imagine 2 nguoi regis va dat cung 1 ten
    /* ca 2 check database va deu ko thay ten nay
    the la 2 nguoi regis va dat cung 1 ten -> nat
    synchronized lm sure chi co 1 thread run cai ham nay tai 1 thoi diem -> 1 trong 2 tk cook va phai dat ten khac
     */
    // xem them ReentrantLock ben placeBid() trong BidService*

    public synchronized User login(LoginPayload payload) {
        String username = normalizeString(payload.username());
        String password = payload.password();

        if (username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Khong de trong username va password");
        }

        //User user = tempDatabase.get(username);
        User user = userDao.getUserByUsername(username);

        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new IllegalArgumentException("Sai ten dang nhap hoac mat khau");
        }

        return user;
    }


    public synchronized User register(RegisterPayload payload) {
        String username = normalizeString(payload.username());
        String role = payload.role();
        String email = payload.email().trim();
        String password = payload.password();

        // todo: validations
        validateRegistration(username, password, email, role);

        //        if (tempDatabase.containsKey(username)) {
//            throw new IllegalArgumentException("Username already exists");
//        }
        if (userDao.userExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        String hashedPassword = BCrypt.hashpw(payload.password(), BCrypt.gensalt());

        User user = createUser(username, hashedPassword, email, role);

        //tempDatabase.put(username, user);
        userDao.saveUser(user);

        logger.info("New user registered: {}", username);

        return user;
    }


    private void validateRegistration(String username, String password, String email, String role) {
        if (username.isBlank() || username.length() < 3) {
            throw new IllegalArgumentException("Username it nhat 3 ky tu");
        }
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password it nhat 8 ky tu");
        }
        // todo : more validations
    }

    private User createUser(String username, String passwordHash, String email, String role) {
        return switch (role) {
            case "SELLER" -> new Seller(username, passwordHash, email);
            case "ADMIN"  -> new Admin(username, passwordHash, email);
            default       -> new Bidder(username, passwordHash, email, 1000000);
        };
    }

    private void seedAdmin() {
        if (!userDao.userExists("admin")) {
            String adminHash = BCrypt.hashpw("admin", BCrypt.gensalt());
            User admin = new Admin("admin", adminHash, "admin@tghauction");
            userDao.saveUser(admin);
        }
    }

    private String normalizeString(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }

}