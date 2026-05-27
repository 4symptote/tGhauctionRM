package com.app.client.network;

import com.app.shared.network.Response;

// Listener = Observer
// Observer interface:
public interface ResponseListener {
    void onResponseReceived(Response response);
}