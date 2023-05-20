package com.njdaeger.authenticationhub.discord;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record DiscordUserProfile(String snowflake, String username, String discriminator) {

    public String decodedUsername() {
        return new String(Base64.getDecoder().decode(username), StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DiscordUserProfile other) {
            return other.snowflake.equalsIgnoreCase(snowflake) && other.username.contentEquals(username) && other.discriminator.contentEquals(discriminator);
        }
        return false;
    }
}
