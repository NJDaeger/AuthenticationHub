package com.njdaeger.authenticationhub;

import com.njdaeger.authenticationhub.database.IDatabase;
import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.RequestException;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import spark.Request;
import spark.Route;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Represents an application that can be authorized and connected to a Minecraft account.
 */
public abstract class Application<T> {

    IDatabase database;
    protected File appConfigFile;
    private Configuration appConfig;
    protected Boolean canBeLoaded = null;

    public Application() {
        this(false);
    }

    public Application(boolean noConfig) {
        Logger logger = AuthenticationHub.getInstance().getLogger();
        if (!noConfig) appConfig = createConfig();

        if (canBeLoaded == null) {
            canBeLoaded = true;
            this.database = AuthenticationHub.getInstance().getDatabase();
            if (database.getApplicationId(this) == -1) database.createApplication(this);
            logger.info(getUniqueName() + " application initialization complete.");
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
     * Function to call when the client connects to the {@link Application#getUniqueName()} route.
     * This should handle any kind of calls needed for connecting a minecraft account to a given user.
     */
//    public abstract JsonObject connect(UUID userId) throws IOException, InterruptedException;

    public abstract String getConnectionUrl(AuthSession session);

    public abstract void handleCallback(Request req, UUID userId, AuthSession session) throws RequestException, IOException, InterruptedException;

    public boolean hasConnection(UUID user) {
        return database.getUserToken(this, user) != null;
    }

    public T getConnection(UUID user) {
        return null;//todo make a rowtransformer thing T that transforms a database result into something useful for this application
    }

    /**
     * Get the associated configuration file with this application. If a configuration file does not exist yet,
     * the
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
