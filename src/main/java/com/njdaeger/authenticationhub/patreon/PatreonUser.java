package com.njdaeger.authenticationhub.patreon;

import com.njdaeger.authenticationhub.database.ISavedConnection;
import com.njdaeger.authenticationhub.database.SaveData;
import org.bukkit.Bukkit;

import java.util.UUID;

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
    private final int id;
    @SaveData(columnOrder = 6, columnType = "int")
    private int pledge;

    PatreonUser(String refreshToken, String accessToken, long expiration, String tokenType, String scope, int id, int pledgeCents) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiration = expiration;
        this.tokenType = tokenType;
        this.pledge = pledgeCents;
        this.scope = scope;
        this.id = id;
    }

    void updateUser(UUID userId, String refreshToken, String accessToken, long expiration, String tokenType, String scope, int pledge) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiration = expiration;
        this.tokenType = tokenType;
        this.scope = scope;
        this.pledge = pledge;
        Bukkit.getPluginManager().callEvent(new PatreonUserUpdateEvent(userId, this));
    }

    void updateUserPledge(UUID userId, int pledge) {
        this.pledge = pledge;
        Bukkit.getPluginManager().callEvent(new PatreonUserUpdateEvent(userId, this));
    }

    /**
     * @return The refresh token used to get a new access token.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * @return The access token used to access the Patreon API.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * @return The expiration date in ms of the access token.
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * @return The type of the access token.
     */
    public String getTokenType() {
        return tokenType;
    }

    /**
     * @return The scope of the access token.
     */
    public String getScope() {
        return scope;
    }

    /**
     * @return The ID of this Patreon user.
     */
    public int getPatreonUserId() {
        return id;
    }

    /**
     * @return If this Patreon user's access token is expired.
     */
    public boolean isExpired() {
        return expiration <= System.currentTimeMillis();
    }

    /**
     * @return If this Patreon user's access token is almost expired. (1 day or less before expiration)
     */
    public boolean isAlmostExpired() {
        return expiration < System.currentTimeMillis() || expiration - System.currentTimeMillis() < (1000 * 60 * 60 * 24);
    }

    /**
     * @return The amount of pledge this Patreon user has in cents.
     */
    public int getPledgingAmount() {
        return pledge;
    }

    /**
     * @return How much time is left in ms before this Patreon user's access token expires in a human readable format.
     */
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
