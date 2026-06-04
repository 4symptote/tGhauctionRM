package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.testutil.TestSupport;
import com.app.client.util.SceneManager;
import com.app.shared.model.user.Bidder;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LoginControllerTest {

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
    void handleLoginRejectsBlankCredentials() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        LoginController controller = controller();
        controller.initialize();
        setField(controller, "usernameField", new TextField(" "));
        PasswordField passwordField = new PasswordField();
        passwordField.setText(" ");
        setField(controller, "passwordField", passwordField);
        Label errorLabel = new Label();
        setField(controller, "errorLabel", errorLabel);

        invoke(controller, "handleLogin", new Class[]{ActionEvent.class}, new ActionEvent());

        assertThat(errorLabel.getText()).isEqualTo("nhap ten va pw");
        verify(networkClient).addListener(controller);
    }

    @Test
    void handleLoginSendsLoginRequestForValidCredentials() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        LoginController controller = controller();
        controller.initialize();
        setField(controller, "usernameField", new TextField("alice"));
        PasswordField passwordField = new PasswordField();
        passwordField.setText("secret123");
        setField(controller, "passwordField", passwordField);
        setField(controller, "errorLabel", new Label());

        invoke(controller, "handleLogin", new Class[]{ActionEvent.class}, new ActionEvent());

        verify(networkClient).sendRequest(new Request(Request.RequestType.LOGIN, new com.app.shared.network.payload.LoginPayload("alice", "secret123")));
    }

    @Test
    void onResponseReceivedHandlesSuccessAndFailure() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        SessionModel sessionModel = SessionModel.getInstance();
        LoginController controller = controller();
        controller.initialize();
        Label errorLabel = new Label();
        setField(controller, "errorLabel", errorLabel);

        User user = new Bidder("alice", "secret123", "alice@example.com", 100.0, 0.0);
        controller.onResponseReceived(new Response(Response.ResponseType.GENERIC_SUCCESS, true, "ok", user));

        assertThat(errorLabel.getText()).isEqualTo("Welcome alice");
        assertThat(sessionModel.getCurrentUser()).isSameAs(user);
        verify(sceneManager).switchScene("/view/fxml/MainLayout.fxml");

        controller.onResponseReceived(new Response(Response.ResponseType.GENERIC_ERROR, false, "bad login", null));
        assertThat(errorLabel.getText()).isEqualTo("bad login");
    }

    @Test
    void switchToRegisterRemovesListenerAndSwitchesScene() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        LoginController controller = controller();
        controller.initialize();

        invoke(controller, "switchToRegister", new Class[]{ActionEvent.class}, new ActionEvent());

        verify(networkClient).removeListener(controller);
        verify(sceneManager).switchScene("/view/fxml/RegisterView.fxml");
    }

    private LoginController controller() {
        LoginController controller = new LoginController();
        setField(controller, "usernameField", new TextField());
        setField(controller, "passwordField", new PasswordField());
        setField(controller, "errorLabel", new Label());
        setField(controller, "loginButton", new Button());
        return controller;
    }

    private void setField(Object target, String name, Object value) {
        TestSupport.setField(target, name, value);
    }

    private void invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(target, args);
    }
}
