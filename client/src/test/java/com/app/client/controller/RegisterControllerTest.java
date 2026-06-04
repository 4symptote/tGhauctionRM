package com.app.client.controller;

import com.app.client.model.SessionModel;
import com.app.client.network.NetworkClient;
import com.app.client.testutil.TestSupport;
import com.app.client.util.SceneManager;
import com.app.shared.model.user.Seller;
import com.app.shared.model.user.User;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RegisterControllerTest {

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
    void handleRegisterRejectsIncompleteForm() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        RegisterController controller = controller();
        controller.initialize();

        invoke(controller, "handleRegister", new Class[]{ActionEvent.class}, new ActionEvent());

        Label errorLabel = (Label) getField(controller, "errorLabel");
        assertThat(errorLabel.getText()).isEqualTo("Please fill out all fields.");
    }

    @Test
    void handleRegisterSendsRegistrationRequest() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        RegisterController controller = controller();
        controller.initialize();
        ((TextField) getField(controller, "usernameField")).setText("alice");
        ((TextField) getField(controller, "emailField")).setText("alice@example.com");
        ((PasswordField) getField(controller, "passwordField")).setText("secret123");
        ((ComboBox<String>) getField(controller, "roleComboBox")).setValue("SELLER");

        invoke(controller, "handleRegister", new Class[]{ActionEvent.class}, new ActionEvent());

        verify(networkClient).sendRequest(new Request(
                Request.RequestType.REGISTER,
                new com.app.shared.network.payload.RegisterPayload("alice", "secret123", "alice@example.com", "SELLER")));
    }

    @Test
    void onResponseReceivedUpdatesSessionAndSceneOnSuccess() {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        SessionModel sessionModel = SessionModel.getInstance();
        RegisterController controller = controller();
        controller.initialize();
        Label errorLabel = (Label) getField(controller, "errorLabel");

        User user = new Seller("alice", "hash", "alice@example.com", 0.0);
        controller.onResponseReceived(new Response(Response.ResponseType.GENERIC_SUCCESS, true, "ok", user));

        assertThat(sessionModel.getCurrentUser()).isSameAs(user);
        assertThat(errorLabel.getText()).isEqualTo("Account created! Logging you in...");
        verify(networkClient).removeListener(controller);
        verify(sceneManager).switchScene("/view/fxml/MainLayout.fxml");
    }

    @Test
    void switchToLoginRemovesListenerAndLoadsLoginView() throws Exception {
        NetworkClient networkClient = mock(NetworkClient.class);
        SceneManager sceneManager = mock(SceneManager.class);
        TestSupport.setStaticField(NetworkClient.class, "instance", networkClient);
        TestSupport.setStaticField(SceneManager.class, "instance", sceneManager);

        RegisterController controller = controller();
        controller.initialize();

        invoke(controller, "switchToLogin", new Class[]{ActionEvent.class}, new ActionEvent());

        verify(networkClient).removeListener(controller);
        verify(sceneManager).switchScene("/view/fxml/LoginView.fxml");
    }

    private RegisterController controller() {
        RegisterController controller = new RegisterController();
        TestSupport.setField(controller, "usernameField", new TextField());
        TestSupport.setField(controller, "emailField", new TextField());
        TestSupport.setField(controller, "passwordField", new PasswordField());
        TestSupport.setField(controller, "roleComboBox", new ComboBox<>());
        TestSupport.setField(controller, "errorLabel", new Label());
        TestSupport.setField(controller, "registerButton", new Button());
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
