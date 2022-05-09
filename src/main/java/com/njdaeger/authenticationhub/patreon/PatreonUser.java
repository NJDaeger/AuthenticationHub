package com.njdaeger.authenticationhub.patreon;

import com.njdaeger.authenticationhub.database.ISavedConnection;
import com.njdaeger.authenticationhub.database.SaveData;

public class PatreonUser implements ISavedConnection {

    @SaveData(columnOrder = 0, columnType = "varchar(512)")
    private final String refreshToken;
    @SaveData(columnOrder = 1, columnType = "varchar(4096)")
    private final String accessToken;
    @SaveData(columnOrder = 2, columnType = "bigint")
    private final long expiration;
    @SaveData(columnOrder = 3)
    private final String tokenType;
    @SaveData(columnOrder = 4, columnType = "varchar(128)")
    private final String scope;

    public PatreonUser(String refreshToken, String accessToken, long expiration, String tokenType, String scope) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.expiration = expiration;
        this.tokenType = tokenType;
        this.scope = scope;
    }

}
