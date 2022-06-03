package com.njdaeger.authenticationhub;

import com.njdaeger.authenticationhub.database.IDatabase;
import com.njdaeger.authenticationhub.database.ISavedConnection;
import com.njdaeger.authenticationhub.database.SaveData;
import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.RequestException;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import spark.Request;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Represents an application that can be authorized and connected to a Minecraft account.
 */
public abstract class Application<T extends ISavedConnection> {

    protected IDatabase database;
    protected File appConfigFile;
    private Configuration appConfig;
    protected AuthenticationHubConfig authHubConfig;
    protected Boolean canBeLoaded = null;
    protected Plugin plugin;

    public Application(Plugin plugin) {
        this(plugin, false);
    }

    public Application(Plugin plugin, boolean noConfig) {
        Logger logger = AuthenticationHub.getInstance().getLogger();
        if (!noConfig) appConfig = createConfig();

        if (canBeLoaded == null) {
            canBeLoaded = true;
            this.database = AuthenticationHub.getInstance().getDatabase();
            this.authHubConfig = AuthenticationHub.getInstance().getAuthHubConfig();
            if (database.getApplicationId(this) == -1) database.createApplication(this);
            this.plugin = plugin;
            logger.info("Loaded application " + getUniqueName());
        }
    }

    /**
     * The name of the application to show up in the dropdown on the hub webpage.
     * @return The name of the service/application being registered
     */
    public abstract String getApplicationName();

    /**
     * The unique name of the application. This is used for the following:
     *
     * * The name of the route for this application
     * * The database table for this application
     * * The name of the config file for this application
     *
     * @return The unique name of this application.
     */
    public abstract String getUniqueName();

    /**
     * Get the URL the AuthSession should use when attempting to connect this application.
     *
     * This is used on the web interface as a link when the user clicks the application button
     *
     * @param session The AuthSession trying to connect to this application
     * @return The connection URL
     */
    public abstract String getConnectionUrl(AuthSession session);

    /**
     * What this application runs when processing a callback request specified in the connection URL above.
     * @param req The request
     * @param userId The UUID of the user whose request is being process
     * @param session The AuthSession of the user whose request is being processed
     * @throws RequestException If there was an error processing this request.
     * @throws IOException
     * @throws InterruptedException
     */
    public abstract void handleCallback(Request req, UUID userId, AuthSession session) throws RequestException, IOException, InterruptedException;

    public boolean hasConnection(UUID user) {
        return database.getUserId(user) != -1 && database.getUserConnections(user).contains(database.getApplicationId(this));
    }

    /**
     * Get a connected user from this application.
     * @param user The user to get the connection of.
     * @return The connected user, or null if the user is not connected to this application.
     */
    public abstract T getConnection(UUID user);

    /**
     * Remove a specific user from this application.
     * @param user The user to remove
     * @return True if the user was removed, false if the user was not connected to this application
     */
    public boolean removeConnection(UUID user) {
        return database.removeUserConnection(this, user);
    }

    /**
     * The class type of the saved connection for this application.
     * @return The class type of the saved connection for this application.
     */
    public abstract Class<T> getSavedDataClass();

    /**
     * Gets the list of fields that are to be saved from the SavedDataClass. (This is the list of fields that are annotated with @SaveData)
     * @return The list of fields that are to be saved from the SavedDataClass.
     */
    public List<Field> getSavedDataFields() {
        return Arrays.stream(getSavedDataClass().getDeclaredFields()).filter(field -> field.isAnnotationPresent(SaveData.class)).toList();
    }

    /**
     * Get the specified fieldNames of the SavedDataFields that are to name the database columns. (Defaults to the field name, but can be overridden by the @SaveData annotation's fieldName attribute)
     * @return The specified fieldNames of the SavedDataFields that are to name the database columns.
     */
    public List<String> getSavedDataFieldNames() {
        var fields = Arrays.stream(getSavedDataClass().getDeclaredFields()).filter(field -> field.isAnnotationPresent(SaveData.class)).toList();
        return fields.stream().map(field -> {
            var annotation = field.getAnnotation(SaveData.class);
            return annotation.fieldName().isEmpty() ? field.getName() : annotation.fieldName();
        }).toList();
    }

    /**
     * Gets the list of datatypes for each SavedDataField. This list should match a constructor in the SavedDataClass that takes the same list of datatypes in the same order.
     * @return The list of datatypes for each SavedDataField.
     */
    public List<? extends Class<?>> getSavedDataFieldTypes() {
        var fields = Arrays.stream(getSavedDataClass().getDeclaredFields()).filter(field -> field.isAnnotationPresent(SaveData.class)).toList();
        return fields.stream().map(Field::getType).toList();
    }

    /**
     * Get the associated configuration file with this application. If a configuration file does not exist yet,
     * it will be created.
     * @return The configuration file associated with this application.
     */
    public Configuration getAppConfig() {
        if (appConfig == null) appConfig = createConfig();
        return appConfig;
    }

    /**
     * Create a configuration file for this application.
     * @return The newly created configuration file, or the existing configuration if one exists already.
     */
    private Configuration createConfig() {
        if (appConfig != null) return appConfig;
        this.appConfigFile = new File(AuthenticationHub.getInstance().getDataFolder().getAbsoluteFile() + File.separator + getUniqueName() + ".yml");
        if (!appConfigFile.exists()) {
            try {
                appConfigFile.createNewFile();
            } catch (IOException e) {
                AuthenticationHub.getInstance().getLogger().warning("The configuration file for the application '" + getUniqueName() + "' was unable to be created or loaded.");
                canBeLoaded = false;
                appConfig = null;
            }
        }
        return appConfig = YamlConfiguration.loadConfiguration(appConfigFile);
    }

}
