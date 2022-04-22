package com.njdaeger.authenticationhub.database;

import com.njdaeger.authenticationhub.Application;

import java.util.List;
import java.util.UUID;

public interface IDatabase {

    void createDatabase();

    /**
     * Creates an application specific table in the Authentication Hub database. This also assigns an ID for this application. If the application
     * table exists already, nothing will occur.
     * @param application The application
     */
    int createApplication(Application application);

    int getApplicationId(Application application);

    int saveUser(UUID userId);

    int getUserId(UUID userId);

    List<Integer> getUserConnections(UUID userId);

    <T extends ISavedResponse> void saveUserConnection(Application<T> application, UUID uuid, T response);

    <T extends ISavedResponse> T getUserConnection(Application<T> application, UUID uuid);

    boolean removeUserToken(Application application, UUID uuid);

    void close();

    void save();

}
