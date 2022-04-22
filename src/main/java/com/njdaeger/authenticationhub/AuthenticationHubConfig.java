package com.njdaeger.authenticationhub;

import com.njdaeger.authenticationhub.database.StorageHandler;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class AuthenticationHubConfig {

    private Configuration config;
    private final AuthenticationHub plugin;

    public AuthenticationHubConfig(AuthenticationHub plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Get the storage handler currently enabled for Authentication Hub.
     * @return The storage handler
     */
    public StorageHandler getStorageHandler() {
        String storage = config.getString("storage-handler", "YML");
        StorageHandler<?> type;
        try {
            type = StorageHandler.getStorageHandler(storage.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = StorageHandler.YML;
            plugin.getLogger().warning("The value for \"storage-type\" could not be parsed. Defaulting to " + type.getNiceName());
        }
        return type;
    }

    /**
     * Get the amount of time an AuthSession is authorized for in milliseconds.
     * @return The amount of time the Auth Session is authorized for.
     */
    public long getSessionTimeoutMilliseconds() {
        long timeoutSeconds = config.getLong("session-timeout");
        if (timeoutSeconds < 60) {
            timeoutSeconds = 600; //If the user sets this to less than 1 minute, im going to assume they probably dont know what they are doing, or they didnt read the comment.
            plugin.getLogger().warning("The value for \"session-timeout\" could either not be parsed, or is less than 60. Defaulting to 10 minutes.");
        }
        return timeoutSeconds * 1000;
    }

    /**
     * Gets the URL that opens the hub webpage.
     * @return The hub url
     */
    public String getHubUrl() {
        return config.getString("hub-url", null);
    }

    /**
     * Get the host for the storage database
     * @return The database host
     */
    public String getDatabaseHost() {
        String host = config.getString("db-host");
        if (host == null) {
            host = "127.0.0.1";
            plugin.getLogger().warning("The value for \"db-host\" could not be parsed. Defaulting to " + host);
        }
        return host;
    }

    /**
     * Get the port for the storage database
     * @return The database port
     */
    public int getDatabasePort() {
        int port = config.getInt("db-port");
        if (port <= 0) {
            port = 3306;
            plugin.getLogger().warning("The value for \"db-port\" could not be parsed. Defaulting to " + port);
        }
        return port;
    }

    /**
     * Get the name of the database
     * @return The database name
     */
    public String getDatabaseName() {
        String name = config.getString("db-name");
        if (name == null) {
            name = "authhub";
            plugin.getLogger().warning("The value for \"db-name\" could not be parsed. Defaulting to " + name);
        }
        return name;
    }

    /**
     * The username to log into the database.
     * @return The database username
     */
    public String getDatabaseUsername() {
        String username = config.getString("db-username");
        if (username == null) {
            username = "root";
            plugin.getLogger().warning("The value for \"db-username\" could not be parsed. Defaulting to " + username);
        }
        return username;
    }

    /**
     * The password to log into the database.
     * @return The database password
     */
    public String getDatabasePassword() {
        String password = config.getString("db-host");
        if (password == null) {
            password = "password";
            plugin.getLogger().warning("The value for \"db-password\" could not be parsed. Defaulting to " + password);
        }
        return password;
    }

    /**
     * The prefix to add onto tables in the database
     * @return The database table prefix
     */
    public String getDatabasePrefix() {
        return config.getString("db-prefix", "");
    }

    /**
     * Get the port the webserver should run on
     * @return The webserver port
     */
    public int getWebserverPort() {
        int port = config.getInt("webserver-port");
        if (port <= 0) {
            port = 4567;
            plugin.getLogger().warning("The value for \"webserver-port\" could not be parsed. Defaulting to " + port);
        }
        return port;
    }

    /**
     * Reload the database
     */
    public void reload() {
        File appConfigFile = new File(plugin.getDataFolder().getAbsoluteFile() + File.separator + "config.yml");
        if (!appConfigFile.exists()) {
            plugin.saveDefaultConfig();
            plugin.getLogger().info("Created configuration file.");
//            try {
//                appConfigFile.createNewFile();
//            } catch (IOException e) {
//                plugin.getLogger().severe("The configuration file was unable to be loaded.");
//                e.printStackTrace();
//            }
        }
        this.config = YamlConfiguration.loadConfiguration(appConfigFile);
    }

    /**
     * Check if the configuration file is loaded.
     * @return True if the config file is loaded.
     */
    public boolean isLoaded() {
        return this.config != null;
    }

}
