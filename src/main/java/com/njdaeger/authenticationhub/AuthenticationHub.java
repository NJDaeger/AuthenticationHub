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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.stream.Stream;

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
//        copyToPluginFolder("web/app.js");
//        copyToPluginFolder("web/index.css");
//        copyToPluginFolder("web/bg.png");
//        copyToPluginFolder("web/bg-blue.png");
//        copyToPluginFolder("web/bg-dark.png");
        copyWebDir();
        File htmlFile = new File(getDataFolder() + File.separator + "web" + File.separator + "index.html");

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

    private void copyWebDir() {
        try {
            URL url = AuthenticationHub.class.getResource("/web");
            if (url == null) throw new RuntimeException("Unable to locate web folder in jar file.");
            URI uri = url.toURI();
            Path path;

            if (uri.getScheme().equals("jar")) {
                System.out.println("TEST");
                FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
                path = fs.getPath("/web");
            } else {
                System.out.println("TEST2");
                path = Paths.get(uri);
            }

            Files.walk(path, 1).filter(pth -> !pth.toString().equals("/web")).forEach(pth -> copyToPluginFolder(pth.toString().replaceFirst("/", "")));
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    private File copyToPluginFolder(String dirInJar) {
        System.out.println(dirInJar);
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
