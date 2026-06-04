package com.app.server.network;

import com.app.server.network.handler.RequestHandler;
import com.app.shared.network.Request;
import com.app.shared.network.Response;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestRouterTest {

    @Test
    void routeReturnsErrorForUnknownRequestType() {
        RequestRouter router = new RequestRouter();

        Response response = router.route(new Request(null, null), mock(ClientHandler.class));

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Unknown request type");
    }

    @Test
    void routeDelegatesToHandlerForKnownType() throws Exception {
        RequestRouter router = new RequestRouter();
        ClientHandler client = mock(ClientHandler.class);
        RequestHandler handler = mock(RequestHandler.class);
        Response expected = new Response(true, "ok", null);
        when(handler.handle(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(client))).thenReturn(expected);

        @SuppressWarnings("unchecked")
        Map<Request.RequestType, RequestHandler> handlers = (Map<Request.RequestType, RequestHandler>) getField(router, "handlers");
        handlers.put(Request.RequestType.LOGOUT, handler);

        Response response = router.route(new Request(Request.RequestType.LOGOUT, null), client);

        assertThat(response).isSameAs(expected);
    }

    private Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
