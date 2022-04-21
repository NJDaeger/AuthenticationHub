package com.njdaeger.authenticationhub.web;

import com.njdaeger.authenticationhub.Application;

import java.util.Base64;
import java.util.UUID;

public final class AuthSession {

    private final UUID userId;
    private boolean authorized;
    private final long sessionStart;
    private String authToken;
    private final String ip;

    public AuthSession(UUID userId, String ip) {
        this.ip = ip;
        this.userId = userId;
        this.authorized = false;
        this.sessionStart = System.currentTimeMillis();
    }

    String getIpAddress() {
        return ip;
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

    public String getEncodedState(Application application) {
        return Base64.getUrlEncoder().encodeToString((application.getUniqueName() + "|" + userId + "|" + authToken).getBytes());
    }

}
