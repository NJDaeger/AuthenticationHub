package com.njdaeger.authenticationhub.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.bukkit.plugin.Plugin;
import spark.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static spark.Service.ignite;
import static spark.Spark.*;

public class WebApplication {

    private final File index;
    private final Plugin plugin;
    private final Service webService;
    private final Map<UUID, String> verificationMap;

    public WebApplication(Plugin plugin, File index) {
        this.verificationMap = new HashMap<>();
        this.plugin = plugin;
        this.index = index;
        plugin.getLogger().info("Initializing webserver");

        BasicConfigurator.configure();
        BasicConfigurator.configure();
        try {

            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/log4j.properties"));
            PropertyConfigurator.configure(props);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*Logger.getRootLogger().*/

        staticFiles.externalLocation(index.getParentFile().getAbsolutePath());
        this.webService = ignite();
        System.out.println(index.getAbsolutePath());

        initExceptionHandler((e) -> {
            plugin.getLogger().warning("Webserver initialization failed");
            e.printStackTrace();
        });

        index();
        postTest();
        validateUUID();
    }

    public Service getWebService() {
        return webService;
    }

    public void index() {
        get("/", (req, res) -> Files.readString(index.toPath()));
    }

    public void postTest() {
        post("/test", (req, res) -> {
            plugin.getLogger().info(req.body());
            return "this is returned";
        });
    }

    public void validateUUID() {
        get("/validate", "application/json", (req, res) -> {
            res.type("application/json");
            String idString = req.queryParams("uuid");
            UUID uuid;
            try {
                uuid = UUID.fromString(idString);
            } catch (IllegalArgumentException e) {
                res.header("content-type", "application/json");
                res.status(400);
                return createObject("error", "Malformed UUID");
            }
            JsonElement obj = readJsonFromUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replaceAll("-", ""));
            if (obj instanceof JsonNull) {
                res.header("content-type", "application/json");
                res.status(404);
                return createObject("error", "There was a problem verifying this Minecraft Account.");
            }
            res.header("content-type", "application/json");
            res.status(200);
            return obj;
        });
    }

//    public void verifyUser() {
//        get("/verify", "application/json", (req, res) -> {
//            res.type("application/json");
//
//        });
//    }

    public JsonObject createObject(String key, String val) {
        JsonObject obj = new JsonObject();
        obj.addProperty(key, val);
        return obj;
    }

    public JsonElement readJsonFromUrl(String url) {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            return new JsonParser().parse(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
