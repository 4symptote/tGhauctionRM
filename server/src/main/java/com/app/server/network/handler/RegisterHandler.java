package com.app.server.network.handler;

import com.app.server.network.ClientHandler;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

public class RegisterHandler implements RequestHandler {
    @Override
    public Response handle(Request request, ClientHandler client) {
        System.out.println("HANDLING"); //TESST
        return null;
    }
}
