package com.njdaeger.authenticationhub;

import com.google.common.io.ByteStreams;
import com.njdaeger.authenticationhub.web.WebApplication;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Main plugin class
 */
public final class AuthenticationHub extends JavaPlugin {

    private WebApplication webapp = null;
    private BukkitTask task = null;
    private static ApplicationRegistry registry;
    private static AuthenticationHub instance;

    @Override
    public void onEnable() {
        instance = this;
        new File(getDataFolder(), "web").mkdirs();
        copyToPluginFolder("web/app.js");
        copyToPluginFolder("web/index.css");
        copyToPluginFolder("web/bg.png");
        copyToPluginFolder("web/bg-blue.png");
        copyToPluginFolder("web/bg-dark.png");
        File htmlFile = copyToPluginFolder("web/index.html");

        task = Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            webapp = new WebApplication(this, htmlFile);
        });

        registry = new ApplicationRegistry(this);
    }

    @Override
    public void onDisable() {
        webapp.getWebService().stop();
        task.cancel();
        registry = null;
    }

    /**
     * Get the ApplicationRegistry where all authorizable applications are registered.
     * @return The ApplicationRegistry
     */
    public static ApplicationRegistry getApplicationRegistry() {
        return registry;
    }

    static AuthenticationHub getInstance() {
        return instance;
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
