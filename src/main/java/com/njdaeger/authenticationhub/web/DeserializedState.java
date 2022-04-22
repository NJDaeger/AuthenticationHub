package com.njdaeger.authenticationhub.web;

import java.util.UUID;

public record DeserializedState(String application, String ip, UUID uuid, String authCode) {
}
