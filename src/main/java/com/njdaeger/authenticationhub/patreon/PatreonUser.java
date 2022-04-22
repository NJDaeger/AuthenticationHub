package com.njdaeger.authenticationhub.patreon;

import com.njdaeger.authenticationhub.database.ISavedConnection;
import com.njdaeger.authenticationhub.database.SaveData;

public class PatreonUser implements ISavedConnection {

    @SaveData
    private final String refreshToken;
    @SaveData
    private final String accessToken;
    @SaveData
    private final long expiration;
    @SaveData
    private final String tokenType;
    @SaveData
    private final String scope;

    public PatreonUser(String refreshToken, String accessToken, long expiration, String tokenType, String scope) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiration = expiration;
        this.tokenType = tokenType;
        this.scope = scope;
    }

}
