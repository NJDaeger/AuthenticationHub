package com.njdaeger.authenticationhub;

import com.google.common.io.ByteStreams;
import com.njdaeger.authenticationhub.database.IDatabase;
import com.njdaeger.authenticationhub.discord.DiscordApplication;
import com.njdaeger.authenticationhub.patreon.PatreonApplication;
import com.njdaeger.authenticationhub.patreon.PatreonListener;
import com.njdaeger.authenticationhub.test.TestApplication;
import com.njdaeger.authenticationhub.web.WebApplication;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.UUID;

/**
 * Main plugin class
 */
public final class AuthenticationHub extends JavaPlugin {

    private WebApplication webapp = null;
    private AuthenticationHubConfig config;
    private AuthenticationHubListeners listeners;
    private ApplicationRegistry registry;
    private IDatabase database;
    private static AuthenticationHub instance;

    @Override
    public void onEnable() {
        instance = this;
        this.config = new AuthenticationHubConfig(this);
        new File(getDataFolder(), "web").mkdirs();
        copyWebDir();
        this.database = config.getStorageHandler().getDatabase(this);
        database.createDatabase();
        this.registry = new ApplicationRegistry(this);
        //Starting the webserver
        if (config.enableWebServer()) this.webapp = new WebApplication(this, config, registry);

        //Registering command
        try {
            var commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            var commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
            commandMap.register("authhub", new AuthenticationHubCommand(webapp));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        //Registering event listener
        this.listeners = new AuthenticationHubListeners(this, webapp);
        Bukkit.getPluginManager().registerEvents(listeners, this);
        Bukkit.getServicesManager().register(ApplicationRegistry.class, registry, this, ServicePriority.Normal);

        config.getEnabledIntegrations().forEach(name -> {
            if (name.equalsIgnoreCase("patreon")) getApplicationRegistry().addApplication(new PatreonApplication(this));
            if (name.equalsIgnoreCase("discord")) getApplicationRegistry().addApplication(new DiscordApplication(this));
            if (name.equalsIgnoreCase("test")) getApplicationRegistry().addApplication(new TestApplication(this));
        });
    }

    @Override
    public void onDisable() {
        database.save();
        database.close();
        if (config.enableWebServer()) webapp.getWebService().stop();
        registry = null;
    }

    /**
     * Get the ApplicationRegistry where all authorizable applications are registered.
     * @return The ApplicationRegistry
     */
    public ApplicationRegistry getApplicationRegistry() {
        return registry;
    }

    /**
     * Get the AuthenticationHub configuration
     * @return The AuthenticationHub config
     */
    public AuthenticationHubConfig getAuthHubConfig() {
        return config;
    }

    /**
     * Get the database that stores user data.
     * @return The storage database
     */
    public IDatabase getDatabase() {
        return database;
    }

    /**
     * Get this plugin instance
     * @return This plugin instance
     */
    public static AuthenticationHub getInstance() {
        return instance;
    }

    private void copyWebDir() {
        try {
            URL url = AuthenticationHub.class.getResource("/web");
            if (url == null) throw new RuntimeException("Unable to locate web folder in jar file.");
            URI uri = url.toURI();
            Path path;

            if (uri.getScheme().equals("jar")) {
                FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
                path = fs.getPath("/web");
            } else {
                path = Paths.get(uri);
            }

            Files.walk(path, 1).filter(pth -> !pth.toString().equals("/web")).forEach(pth -> copyToPluginFolder(pth.toString().replaceFirst("/", "")));
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    private File copyToPluginFolder(String dirInJar) {
        File file = new File(getDataFolder(), dirInJar);
        try {
            file.createNewFile();

            try (InputStream is = getResource(dirInJar)) {
                ByteStreams.copy(is, new FileOutputStream(file));
            } catch (IOException e) {
                throw new RuntimeException("Unable to copy data to " + file.getAbsolutePath());
            }

        } catch (IOException e) {
            throw new RuntimeException("Unable to create " + file.getAbsolutePath());
        }
        return file;
    }

    public void removeLastLogin(UUID uuid) {
        listeners.removeLastLogin(uuid);
    }

}
