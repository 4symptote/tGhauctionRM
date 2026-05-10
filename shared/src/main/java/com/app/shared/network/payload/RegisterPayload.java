package com.app.shared.network.payload;

public record RegisterPayload(String username, String password, String email, String role) {
}
