package com.app.server.service;

import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.payload.LoginPayload;

import com.app.shared.exception.AuthenticationException;

import com.app.shared.network.payload.RegisterPayload;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserManager {
    private static UserManager instance;

    private final Map<String, User> users = new HashMap<>();

    private UserManager() {

    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public synchronized User login(LoginPayload payload) {
        String username = normalizeUsername(payload.username());
        if (username.isBlank() || isBlank(payload.password())) {
            throw new AuthenticationException("Username and password are required.");
        }

        User user = users.get(username);

        // BCrypt extracts the salt and safely verifies the password
        if (user == null || !BCrypt.checkpw(payload.password(), user.getPassword())) {
            throw new AuthenticationException("Wrong username or password.");
        }
        return user;
    }

    public synchronized User register(RegisterPayload payload) {
        String username = normalizeUsername(payload.username());
        String role = normalizeRole(payload.role());

        validateRegistration(username, payload.password(), payload.email(), role);

        if (users.containsKey(username)) {
            throw new IllegalArgumentException("Username already exists.");
        }

        // BCrypt stuff
        String hashedPassword = BCrypt.hashpw(payload.password(), BCrypt.gensalt());

        User user = createUser(username, hashedPassword, payload.email().trim(), role);
        users.put(username, user);
//        saveUser(user)
        return user;
    }


    private User createUser(String username, String passwordHash, String email, String role) {
        return switch (role) {
            case "SELLER" -> new Seller(username, passwordHash, email);
            case "ADMIN" -> new Admin(username, passwordHash, email);
            default -> new Bidder(username, passwordHash, email, 1_000_000.0);
        };
    }


    private void validateRegistration(String username, String password, String email, String role) {
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters.");
        }
        if (isBlank(password) || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters.");
        }
        if (isBlank(email) || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email is required.");
        }
        if (!role.equals("BIDDER") && !role.equals("SELLER")) {
            throw new IllegalArgumentException("Role must be Bidder or Seller.");
        }
    }


    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "BIDDER";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}