package com.njdaeger.authenticationhub.test;

import com.njdaeger.authenticationhub.database.ISavedConnection;
import com.njdaeger.authenticationhub.database.SaveData;

public class TestAppUser implements ISavedConnection {

    @SaveData(columnOrder = 0, columnType = "varchar(512)")
    private String uuid;

    public TestAppUser(String uuid) {
        this.uuid = uuid;
    }

    public String gettUUID() {
        return uuid;
    }

}
