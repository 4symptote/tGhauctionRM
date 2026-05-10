package com.app.shared.network;

import java.io.Serial;

public record Response(boolean success, String message, Object payload) {
    @Serial
    private static final long serialVersionUID = 1L;
}
