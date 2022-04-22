package com.njdaeger.authenticationhub.web;

import java.util.UUID;

/**
 * Represents the state query parameter in its deserialized form.
 */
public record DeserializedState(String application, String ip, UUID uuid, String authCode) {
}
