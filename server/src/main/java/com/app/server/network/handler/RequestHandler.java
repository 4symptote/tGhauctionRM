package com.app.server.network.handler;

import com.app.server.network.ClientHandler;
import com.app.shared.network.Request;
import com.app.shared.network.Response;

public interface RequestHandler {
    Response handle(Request request, ClientHandler client);
}