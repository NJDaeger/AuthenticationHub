package com.njdaeger.authenticationhub.patreon;

import com.njdaeger.authenticationhub.database.ISavedConnection;
import com.njdaeger.authenticationhub.database.SaveData;

public final class PatreonUser implements ISavedConnection {

    @SaveData(columnOrder = 0, columnType = "varchar(512)")
    private String refreshToken;
    @SaveData(columnOrder = 1, columnType = "varchar(4096)")
    private String accessToken;
    @SaveData(columnOrder = 2, columnType = "bigint")
    private long expiration;
    @SaveData(columnOrder = 3)
    private String tokenType;
    @SaveData(columnOrder = 4, columnType = "varchar(128)")
    private String scope;
    @SaveData(columnOrder = 5, columnType = "int")
    private int id;
    @SaveData(columnOrder = 6, columnType = "int")
    private int pledge;

    PatreonUser(String refreshToken, String accessToken, long expiration, String tokenType, String scope, int id, int pledgeCents) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiration = expiration + System.currentTimeMillis(); //This is the day this token expires.
        this.tokenType = tokenType;
        this.pledge = pledgeCents;
        this.scope = scope;
        this.id = id;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    void updateUser(String refreshToken, String accessToken, long expiration, String tokenType, String scope, int pledge) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiration = expiration;
        this.tokenType = tokenType;
        this.scope = scope;
        this.pledge = pledge;
    }

    void updateUserPledge(int pledge) {
        this.pledge = pledge;
    }

    String getAccessToken() {
        return accessToken;
    }

    public long getExpiration() {
        return expiration;
    }

    public int getPatreonUserId() {
        return id;
    }

    public boolean isExpired() {
        return expiration <= System.currentTimeMillis();
    }

    public boolean isAlmostExpired() {
        return expiration < System.currentTimeMillis() || expiration - System.currentTimeMillis() < (1000 * 60 * 60 * 24);
    }

    public int getPledgingAmount() {
        return pledge;
    }

    public String getTimeUntilExpiration() {
        long time = expiration - System.currentTimeMillis();
        if (time < 0) return "User Patreon token has expired.";
        long days = time / (1000 * 60 * 60 * 24);
        long hours = (time - (days * (1000 * 60 * 60 * 24))) / (1000 * 60 * 60);
        long minutes = (time - (days * (1000 * 60 * 60 * 24)) - (hours * (1000 * 60 * 60))) / (1000 * 60);
        long seconds = (time - (days * (1000 * 60 * 60 * 24)) - (hours * (1000 * 60 * 60)) - (minutes * (1000 * 60))) / 1000;
        return days + " days, " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds until user Patreon token expiration.";
    }
}
