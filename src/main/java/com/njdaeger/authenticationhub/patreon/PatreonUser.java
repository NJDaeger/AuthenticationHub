package com.njdaeger.authenticationhub.patreon;

import com.njdaeger.authenticationhub.database.ISavedResponse;
import com.njdaeger.authenticationhub.database.SaveData;

public class PatreonUser implements ISavedResponse {

    @SaveData
    private String refreshToken;
    @SaveData
    private String accessToken;
    @SaveData
    private long expiration;
    @SaveData
    private String tokenType;
    @SaveData
    private String scope;

    public PatreonUser(@SaveData String refreshToken, @SaveData String accessToken, @SaveData long expiration, @SaveData String tokenType, @SaveData String scope) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiration = expiration;
        this.tokenType = tokenType;
        this.scope = scope;
    }

}
