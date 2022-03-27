package com.njdaeger.authenticationhub.web;

import java.util.UUID;

public final class AuthSession {

    private final UUID userId;
    private boolean authorized;
    private final long sessionStart;
    private String authToken;

    public AuthSession(UUID userId) {
        this.userId = userId;
        this.authorized = false;
        this.sessionStart = System.currentTimeMillis();
    }

    public long getSessionStart() {
        return sessionStart;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public String getAuthToken() {
        return authToken;
    }


    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
