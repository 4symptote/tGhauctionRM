package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.testutil.TestSupport;
import com.app.client.util.SceneManager;
import com.app.client.util.ToastUtil;
import com.app.shared.model.user.Admin;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class MainLayoutControllerTest {

    @BeforeAll
    static void startFx() {
        TestSupport.initJavaFx();
    }

    @AfterEach
    void tearDown() {
        TestSupport.setStaticField(NetworkClient.class, "instance", null);
        TestSupport.setStaticField(SceneManager.class, "instance", null);
        TestSupport.setStaticField(SessionModel.class, "instance", null);
    }

    @Test
    void initializePopulatesBidderUiAndNavigatesToDashboard() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        SessionModel sessionModel = SessionModel.getInstance();
        User user = new Bidder("alice", "hash", "alice@example.com", 500.0, 0.0);
        sessionModel.setCurrentUser(user);

        MainLayoutController controller = controller();

        try (MockedStatic<Platform> platform = mockStatic(Platform.class)) {
            platform.when(() -> Platform.runLater(any())).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0, Runnable.class);
                runnable.run();
                return null;
            });

            controller.initialize();
        }

        assertThat(((Label) getField(controller, "welcomeLabel")).getText()).isEqualTo("alice");
        assertThat(((Label) getField(controller, "roleLabel")).getText()).isEqualTo("BIDDER");
        assertThat(((Label) getField(controller, "balanceLabel")).getText()).contains("500.00");
        assertThat(((Button) getField(controller, "myListingsBtn")).isVisible()).isFalse();
        assertThat(((Button) getField(controller, "winningBidsBtn")).isVisible()).isTrue();
        verify(sceneManager).switchScene("/view/fxml/DashboardView.fxml");
        verify(networkClient).addListener(controller);
    }

    @Test
    void updateUserUiShowsSellerRevenueAndNavigationButtons() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        MainLayoutController controller = controller();
        Seller seller = new Seller("seller", "hash", "seller@example.com", 750.0);

        invoke(controller, "updateUserUI", new Class[]{User.class}, seller);

        assertThat(((Label) getField(controller, "welcomeLabel")).getText()).isEqualTo("seller");
        assertThat(((Label) getField(controller, "roleLabel")).getText()).isEqualTo("SELLER");
        assertThat(((Label) getField(controller, "balanceLabel")).getText()).contains("750.00");
        assertThat(((Button) getField(controller, "myListingsBtn")).isVisible()).isTrue();
        assertThat(((Button) getField(controller, "winningBidsBtn")).isVisible()).isFalse();
    }

    @Test
    void updateUserUiForAdminOpensAdminDashboard() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        MainLayoutController controller = controller();
        Admin admin = new Admin("admin", "hash", "admin@example.com");

        invoke(controller, "updateUserUI", new Class[]{User.class}, admin);

        assertThat(((Button) getField(controller, "adminDashboardBtn")).isVisible()).isTrue();
        verify(sceneManager, times(2)).switchScene("/view/fxml/AdminDashboardView.fxml");
    }

    @Test
    void handleLogoutClearsSessionAndSendsLogoutRequest() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        SessionModel sessionModel = SessionModel.getInstance();
        sessionModel.setCurrentUser(new Bidder("alice", "hash", "alice@example.com", 500.0, 0.0));

        MainLayoutController controller = controller();
        controller.initialize();

        invoke(controller, "handleLogout", new Class[0]);

        assertThat(sessionModel.getCurrentUser()).isNull();
        verify(networkClient).removeListener(controller);
        verify(networkClient).sendRequest(new Request(Request.RequestType.LOGOUT, null));
        verify(sceneManager).logoutToLoginScreen();
    }

    @Test
    void onResponseReceivedShowsToastAndRefreshesUi() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        MainLayoutController controller = controller();
        User updatedUser = new Bidder("alice", "hash", "alice@example.com", 650.0, 0.0);

        try (MockedStatic<Platform> platform = mockStatic(Platform.class);
             MockedStatic<ToastUtil> toastUtil = mockStatic(ToastUtil.class)) {
            platform.when(() -> Platform.runLater(any())).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0, Runnable.class);
                runnable.run();
                return null;
            });

            controller.onResponseReceived(new Response(Response.ResponseType.USER_UPDATED, true, "Balance updated", updatedUser));

            assertThat(((Label) getField(controller, "balanceLabel")).getText()).contains("650.00");
            toastUtil.verify(() -> ToastUtil.showToast("Balance updated", ToastUtil.ToastType.INFO));
        }
    }

    private MainLayoutController controller() {
        MainLayoutController controller = new MainLayoutController();
        TestSupport.setField(controller, "dashboardBtn", new Button("Dashboard"));
        TestSupport.setField(controller, "contentArea", new StackPane());
        TestSupport.setField(controller, "welcomeLabel", new Label());
        TestSupport.setField(controller, "roleLabel", new Label());
        TestSupport.setField(controller, "balanceLabel", new Label());
        TestSupport.setField(controller, "myListingsBtn", new Button("My Listings"));
        TestSupport.setField(controller, "winningBidsBtn", new Button("Winning Bids"));
        TestSupport.setField(controller, "createAuctionBtn", new Button("Create"));
        TestSupport.setField(controller, "adminDashboardBtn", new Button("Admin"));
        return controller;
    }

    private Object getField(Object target, String name) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(target, args);
    }
}
