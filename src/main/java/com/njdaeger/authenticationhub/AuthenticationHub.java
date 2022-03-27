package com.njdaeger.authenticationhub;

import com.google.common.io.ByteStreams;
import com.njdaeger.authenticationhub.web.WebApplication;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
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

/**
 * Main plugin class
 */
public final class AuthenticationHub extends JavaPlugin {

    private WebApplication webapp = null;
    private AuthenticationHubConfig config;
    private ApplicationRegistry registry;
    private static AuthenticationHub instance;

    @Override
    public void onEnable() {
        instance = this;
        this.config = new AuthenticationHubConfig(this);
        new File(getDataFolder(), "web").mkdirs();
        copyWebDir();

        //Starting the webserver
        this.webapp = new WebApplication(this);
        this.registry = new ApplicationRegistry(this);

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
        Bukkit.getPluginManager().registerEvents(new AuthenticationHubListeners(webapp), this);
    }

    @Override
    public void onDisable() {
        webapp.getWebService().stop();
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

    static AuthenticationHub getInstance() {
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
}
