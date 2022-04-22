package com.njdaeger.authenticationhub.database.impl;

import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.AuthenticationHub;
import com.njdaeger.authenticationhub.database.IDatabase;
import com.njdaeger.authenticationhub.database.ISavedConnection;

import java.util.List;
import java.util.UUID;

public class SqlDatabase implements IDatabase {

    private final AuthenticationHub plugin;

    public SqlDatabase(AuthenticationHub plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createDatabase() {

    }

    @Override
    public int createApplication(Application application) {
        return 0;
    }

    @Override
    public int getApplicationId(Application application) {
        return 0;
    }

    @Override
    public int createUser(UUID userId) {
        return 0;
    }

    @Override
    public int getUserId(UUID userId) {
        return 0;
    }

    @Override
    public List<Integer> getUserConnections(UUID userId) {
        return null;
    }

    @Override
    public <T extends ISavedConnection> void saveUserConnection(Application<T> application, UUID uuid, T response) {

    }

    @Override
    public <T extends ISavedConnection> T getUserConnection(Application<T> application, UUID uuid) {
        return null;
    }

    @Override
    public boolean removeUserConnection(Application application, UUID uuid) {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public void save() {

    }
}
