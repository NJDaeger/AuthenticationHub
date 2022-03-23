package com.njdaeger.authenticationhub.web;

import java.util.UUID;

public final class AuthSession {

    private final UUID userId;
    private String authToken;
    private boolean authorized;
    private long sessionStart;

    public AuthSession(UUID uuid, String token) {
        this.userId = uuid;
        this.authToken = token;
        this.authorized = false;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public String getAuthToken() {
        return authToken;
    }
}
