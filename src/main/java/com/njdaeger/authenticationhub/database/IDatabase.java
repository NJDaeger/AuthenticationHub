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

    int createUser(UUID userId);

    int getUserId(UUID userId);

    List<String> getUserConnections(UUID userId);

    void createUserConnection(Application application, UUID uuid, String token);

    //todo perhaps this needs to return a generic type T where each application can specify what exactly it stores- T would extend a rowtransformer of some sort that takes the queried row from the application's database and converts it to a useable object.
    String getUserToken(Application application, UUID uuid);

    boolean removeUserToken(Application application, UUID uuid);

    void close();

    void save();

}
