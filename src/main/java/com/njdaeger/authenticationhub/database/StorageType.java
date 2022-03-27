package com.njdaeger.authenticationhub.database;

public enum StorageType {

    SQLITE("SQLite"),
    SQL("SQL"),
    YML("YML");

    private final String niceName;

    StorageType(String niceName) {
        this.niceName = niceName;
    }

    /**
     * Get the nice name of the storage type
     * @return The nice name of the storage type
     */
    public String getNiceName() {
        return niceName;
    }
}
