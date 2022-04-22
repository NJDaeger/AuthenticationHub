package com.njdaeger.authenticationhub.database;

import com.njdaeger.authenticationhub.Application;

import java.util.List;
import java.util.UUID;

/**
 * Represents the Authentication Hub database
 */
public interface IDatabase {

    /**
     * Initializes any necessary database objects, such as tables, files, etc.
     */
    void createDatabase();

    /**
     * Creates an application specific table in the Authentication Hub database. This also assigns an ID for this application. If the application
     * table exists already, the application ID is returned.
     * @param application The application ID
     */
    int createApplication(Application<?> application);

    /**
     * Gets the application ID of a specific application.
     * @param application The application to get the ID of
     * @return The ID of the application, or -1 if the application provided is not registered.
     */
    int getApplicationId(Application<?> application);

    /**
     * Creates a user in the Authentication Hub database. This also assigns an ID to this user. If the user already exists in the database,
     * the users current ID will be returned.
     * @param userId The Minecraft UUID of the user to create an ID for.
     * @return The ID for the given user.
     */
    int createUser(UUID userId);

    /**
     * Gets the database ID of a specific Minecraft User
     * @param userId  The Minecraft UUID of the user to get the ID of
     * @return The database ID for the given user, or -1 if no entry exists for the given UUID.
     */
    int getUserId(UUID userId);

    /**
     * Get a list of user connections for a given UUID.
     * @param userId The UUID to get the connections of.
     * @return A list of application IDs the user has connected to.
     */
    List<Integer> getUserConnections(UUID userId);

    /**
     * Save a user connection to an application's database.
     * @param application The application the user is being saved to
     * @param uuid The UUID of the user being saved.
     * @param response The response to be saved.
     * @param <T> The object that is being saved to the database.
     */
    <T extends ISavedConnection> void saveUserConnection(Application<T> application, UUID uuid, T response);

    /**
     * Get a user connection from an application
     * @param application The name of the application to search from
     * @param uuid The UUID of the user to search for
     * @param <T> The object that has been saved to the database and needs to be deserialized.
     * @return The SavedConnection object
     */
    <T extends ISavedConnection> T getUserConnection(Application<T> application, UUID uuid);

    /**
     * Remove a user connection from an application
     * @param application The application to remove the connection from.
     * @param uuid The UUID of the user to remove from the application
     * @return False if the user does not have a connection to this application, true if it has been removed.
     */
    boolean removeUserConnection(Application<?> application, UUID uuid);

    /**
     * Close the database and any remaining connections
     */
    void close();

    /**
     * Commit any non-committed changes to the database without closing any connections.
     */
    void save();

}
