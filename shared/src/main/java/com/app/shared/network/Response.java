package com.app.shared.network;

import java.io.Serial;
import java.io.Serializable;

public record Response(boolean success, String message, Object payload) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
