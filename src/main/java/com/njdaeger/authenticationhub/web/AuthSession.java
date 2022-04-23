package com.njdaeger.authenticationhub.web;

import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.AuthenticationHub;

import java.sql.Date;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

public final class AuthSession {

    private final UUID userId;
    private boolean authorized;
    private final long sessionStart;
    private String authToken;
    private final String ip;
    private final AuthenticationHub plugin;

    public AuthSession(UUID userId, String ip, AuthenticationHub plugin) {
        this.ip = ip;
        this.userId = userId;
        this.plugin = plugin;
        this.authorized = false;
        this.sessionStart = System.currentTimeMillis();
    }

    /**
     * Get the IP address of user who started this AuthSession
     * @return IP address of user who started this AuthSession
     */
    public String getIpAddress() {
        return ip;
    }

    /**
     * When this session started in milliseconds
     * @return When this session started in milliseconds
     */
    public long getSessionStart() {
        return sessionStart;
    }

    /**
     * Get the time remaining in milliseconds until this session times out.
     * @return The time remaining in milliseconds until this session times out.
     */
    public long getTimeRemaining() {
        return plugin.getAuthHubConfig().getSessionTimeoutMilliseconds() - (System.currentTimeMillis() - sessionStart);
    }

    /**
     * Get the time remaining in a nice HH:mm:ss format.
     * @return Time remaining in a nice format.
     */
    public String getNiceTimeRemaining() {
        var ms = getTimeRemaining();
        return String.format("%d:%02d:%02d", ms / 3600000, (ms % 3600000) / 60000, ((ms % 3600000) % 60000) / 1000);
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
        return Base64.getUrlEncoder().encodeToString((ip + "|" + userId + "|" + authToken + (application == null ? "" : ("|" + application.getUniqueName()))).getBytes());
    }

}
