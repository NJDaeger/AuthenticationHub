package com.njdaeger.authenticationhub.database;

import com.njdaeger.authenticationhub.AuthenticationHub;
import com.njdaeger.authenticationhub.database.impl.SqlDatabase;
import com.njdaeger.authenticationhub.database.impl.YmlDatabase;

import java.util.function.Function;

public class StorageHandler<D extends IDatabase> {

//    SQLITE("SQLite"),
    public static final StorageHandler<SqlDatabase> SQL = new StorageHandler<>("SQL", SqlDatabase::new);
    public static final StorageHandler<YmlDatabase> YML = new StorageHandler<>("YML", YmlDatabase::new);

    private final String niceName;
    private final Function<AuthenticationHub, D> createDatabase;

    StorageHandler(String niceName, Function<AuthenticationHub, D> createDatabase) {
        this.createDatabase = createDatabase;
        this.niceName = niceName;
    }

    /**
     * Get the nice name of the storage type
     * @return The nice name of the storage type
     */
    public String getNiceName() {
        return niceName;
    }

    public IDatabase getDatabase(AuthenticationHub plugin) {
        return createDatabase.apply(plugin);
    }

    public static StorageHandler<?> getStorageHandler(String databaseName) {
        return switch (databaseName) {
            case "SQL" -> SQL;
            case "YML" -> YML;
            default -> null;
        };
    }

}
