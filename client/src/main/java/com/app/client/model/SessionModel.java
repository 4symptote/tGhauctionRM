package com.app.client.model;

import com.app.client.network.NetworkClient;
import com.app.client.network.ResponseListener;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.User;
import com.app.shared.network.Response;

//
public class SessionModel implements ResponseListener {
    private static SessionModel instance;
    private User currentUser;

    // Singleton ofc
    private SessionModel() {
        NetworkClient.getInstance().addListener(this);
    }

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


    public void logout() { this.currentUser = null; }

    @Override
    public void onResponseReceived(Response response) {
        switch (response.type()) {
            case USER_UPDATED -> handleUserUpdateResponse(response);
            default -> {}
        }
    }

    private void handleUserUpdateResponse(Response response) {
        if (response.success() && response.payload() instanceof User user) {
            this.currentUser = user;
        }
    }
}