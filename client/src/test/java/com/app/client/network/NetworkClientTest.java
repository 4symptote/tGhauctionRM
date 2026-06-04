package com.app.client.network;

import com.app.client.testutil.TestSupport;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import javafx.application.Platform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NetworkClientTest {

    @BeforeAll
    static void startFx() {
        TestSupport.initJavaFx();
    }

    @AfterEach
    void tearDown() {
        TestSupport.setStaticField(NetworkClient.class, "instance", null);
    }

    @Test
    void addListenerAvoidsDuplicatesAndRemoveListenerWorks() {
        NetworkClient client = NetworkClient.getInstance();
        ResponseListener listener = mock(ResponseListener.class);

        client.addListener(listener);
        client.addListener(listener);
        client.removeListener(listener);

        assertThat(readListeners(client)).isEmpty();
    }

    @Test
    void isConnectedReflectsSocketAndStreamsState() throws Exception {
        NetworkClient client = NetworkClient.getInstance();
        Socket socket = mock(Socket.class);
        ObjectOutputStream out = mock(ObjectOutputStream.class);
        ObjectInputStream in = mock(ObjectInputStream.class);
        when(socket.isClosed()).thenReturn(false);

        setField(client, "socket", socket);
        setField(client, "out", out);
        setField(client, "in", in);

        assertThat(client.isConnected()).isTrue();

        when(socket.isClosed()).thenReturn(true);
        assertThat(client.isConnected()).isFalse();
    }

    @Test
    void sendRequestWritesObjectWhenConnected() throws Exception {
        NetworkClient client = NetworkClient.getInstance();
        Socket socket = mock(Socket.class);
        ObjectOutputStream out = mock(ObjectOutputStream.class);
        ObjectInputStream in = mock(ObjectInputStream.class);
        when(socket.isClosed()).thenReturn(false);

        setField(client, "socket", socket);
        setField(client, "out", out);
        setField(client, "in", in);

        Request request = new Request(Request.RequestType.LOGOUT, null);
        client.sendRequest(request);

        verify(out).writeObject(request);
        verify(out).flush();
    }

    @Test
    void handleResponseNotifiesListenersOnFxThread() throws Exception {
        NetworkClient client = NetworkClient.getInstance();
        ResponseListener listener = mock(ResponseListener.class);
        client.addListener(listener);

        Method handleResponse = NetworkClient.class.getDeclaredMethod("handleResponse", Response.class);
        handleResponse.setAccessible(true);

        try (MockedStatic<Platform> platform = mockStatic(Platform.class)) {
            platform.when(() -> Platform.runLater(any())).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(0, Runnable.class);
                runnable.run();
                return null;
            });

            Response response = new Response(Response.ResponseType.GENERIC_SUCCESS, true, "ok", null);
            handleResponse.invoke(client, response);

            verify(listener).onResponseReceived(response);
        }
    }

    @Test
    void disconnectClosesAllResources() throws Exception {
        NetworkClient client = NetworkClient.getInstance();
        Socket socket = mock(Socket.class);
        ObjectOutputStream out = mock(ObjectOutputStream.class);
        ObjectInputStream in = mock(ObjectInputStream.class);

        setField(client, "socket", socket);
        setField(client, "out", out);
        setField(client, "in", in);

        client.disconnect();

        verify(in).close();
        verify(out).close();
        verify(socket).close();
    }

    @SuppressWarnings("unchecked")
    private CopyOnWriteArrayList<ResponseListener> readListeners(NetworkClient client) {
        try {
            Field field = NetworkClient.class.getDeclaredField("listeners");
            field.setAccessible(true);
            return new CopyOnWriteArrayList<>((CopyOnWriteArrayList<ResponseListener>) field.get(client));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setField(NetworkClient client, String fieldName, Object value) {
        try {
            Field field = NetworkClient.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(client, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
