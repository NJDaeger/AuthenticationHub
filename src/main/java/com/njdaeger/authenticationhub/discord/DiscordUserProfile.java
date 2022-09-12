package com.njdaeger.authenticationhub.discord;

public record DiscordUserProfile(String snowflake, String username, String discriminator) {

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DiscordUserProfile other) {
            return other.snowflake.equalsIgnoreCase(snowflake) && other.username.contentEquals(username) && other.discriminator.contentEquals(discriminator);
        }
        return false;
    }
}
