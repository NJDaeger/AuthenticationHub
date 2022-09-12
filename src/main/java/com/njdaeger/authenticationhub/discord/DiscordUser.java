package com.njdaeger.authenticationhub.discord;

import com.njdaeger.authenticationhub.database.ISavedConnection;
import com.njdaeger.authenticationhub.database.SaveData;
import org.bukkit.Bukkit;

import java.util.UUID;

public class DiscordUser implements ISavedConnection {

    @SaveData(columnOrder = 0, columnType = "varchar(512)")
    private String refreshToken;
    @SaveData(columnOrder = 1, columnType = "varchar(4096)")
    private String accessToken;
    @SaveData(columnOrder = 2, columnType = "bigint")
    private long expiration;
    @SaveData(columnOrder = 3)
    private String tokenType;
    @SaveData(columnOrder = 4)
    private String scope;
    @SaveData(columnOrder = 6, columnType = "int")
    private final String snowflake;
    @SaveData(columnOrder = 7, columnType = "varchar(128)")
    private String username;
    @SaveData(columnOrder = 8, columnType = "varchar(4)")
    private String discriminator;

    DiscordUser(String refreshToken, String accessToken, long expiration, String tokenType, String scope, String snowflake, String username, String discriminator) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiration = expiration;
        this.tokenType = tokenType;
        this.scope = scope;
        this.snowflake = snowflake;
        this.username = username;
        this.discriminator = discriminator;
    }

    void updateUser(UUID userId, String refreshToken, String accessToken, long expiration, String tokenType, String scope, String username, String discriminator) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiration = expiration;
        this.tokenType = tokenType;
        this.scope = scope;
        this.username = username;
        this.discriminator = discriminator;
        Bukkit.getPluginManager().callEvent(new DiscordUserUpdateEvent(userId, this));
    }

    void updateUsernameAndDisc(UUID userId, String username, String discriminator) {
        this.username = username;
        this.discriminator = discriminator;
        Bukkit.getPluginManager().callEvent(new DiscordUserUpdateEvent(userId, this));
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
     * Get the discord user profile information. (snowflake, username, and discriminator)
     * @return The discord user profile basic information
     */
    public DiscordUserProfile getDiscordProfile() {
        return new DiscordUserProfile(snowflake, username, discriminator);
    }

    /**
     * @return If this Discord user's access token is expired.
     */
    public boolean isExpired() {
        return expiration <= System.currentTimeMillis();
    }

    /**
     * @return If this Discord user's access token is almost expired. (5 days or less before expiration)
     */
    public boolean isAlmostExpired() {
        return expiration < System.currentTimeMillis() || expiration - System.currentTimeMillis() < (1000 * 60 * 60 * 24 * 5);
    }

    /**
     * @return How much time is left in ms before this Discord user's access token expires in a human readable format.
     */
    public String getTimeUntilExpiration() {
        long time = expiration - System.currentTimeMillis();
        if (time < 0) return "User Discord token has expired.";
        long days = time / (1000 * 60 * 60 * 24);
        long hours = (time - (days * (1000 * 60 * 60 * 24))) / (1000 * 60 * 60);
        long minutes = (time - (days * (1000 * 60 * 60 * 24)) - (hours * (1000 * 60 * 60))) / (1000 * 60);
        long seconds = (time - (days * (1000 * 60 * 60 * 24)) - (hours * (1000 * 60 * 60)) - (minutes * (1000 * 60))) / 1000;
        return days + " days, " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds until user Discord token expiration.";
    }

}
