package com.app.client.model;

import com.app.shared.model.user.User;

//
public class SessionModel {
    private static SessionModel instance;
    private User currentUser;

    // Singleton ofc
    private SessionModel() {}

    public static SessionModel getInstance() {
        if (instance == null) {
            instance = new SessionModel();
        }
        return instance;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public void logout() {

        this.currentUser = null;
    }
}