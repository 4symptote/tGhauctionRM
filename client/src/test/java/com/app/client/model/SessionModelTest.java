package com.app.client.model;

import com.app.client.network.NetworkClient;
import com.app.client.testutil.TestSupport;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.User;
import com.app.shared.network.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SessionModelTest {

    @AfterEach
    void tearDown() {
        TestSupport.setStaticField(SessionModel.class, "instance", null);
        TestSupport.setStaticField(NetworkClient.class, "instance", null);
    }

    @Test
    void getInstanceRegistersListenerOnNetworkClient() {
        NetworkClient networkClient = mock(NetworkClient.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);

        SessionModel sessionModel = SessionModel.getInstance();

        verify(networkClient).addListener(sessionModel);
    }

    @Test
    void onResponseReceivedUpdatesCurrentUserOnlyForSuccessfulUserUpdate() {
        NetworkClient networkClient = mock(NetworkClient.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);

        SessionModel sessionModel = SessionModel.getInstance();
        User user = new Bidder("alice", "secret123", "alice@example.com", 100.0, 0.0);

        sessionModel.onResponseReceived(new Response(Response.ResponseType.USER_UPDATED, true, "ok", user));

        assertThat(sessionModel.getCurrentUser()).isSameAs(user);

        sessionModel.onResponseReceived(new Response(Response.ResponseType.USER_UPDATED, false, "bad", null));
        assertThat(sessionModel.getCurrentUser()).isSameAs(user);
    }

    @Test
    void logoutClearsCurrentUser() {
        NetworkClient networkClient = mock(NetworkClient.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);

        SessionModel sessionModel = SessionModel.getInstance();
        sessionModel.setCurrentUser(new Bidder("alice", "secret123", "alice@example.com", 100.0, 0.0));
        sessionModel.logout();

        assertThat(sessionModel.getCurrentUser()).isNull();
    }
}
