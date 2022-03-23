package com.njdaeger.authenticationhub.web;

import com.google.gson.*;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.bukkit.plugin.Plugin;
import spark.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static spark.Service.ignite;
import static spark.Spark.*;

/**
 *
 */
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

        initExceptionHandler((e) -> {
            plugin.getLogger().warning("Webserver initialization failed");
            e.printStackTrace();
        });

        index();
        postTest();
        validate();
        pullInfo();
        authorize();
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

    /**
     * When the user first loads the webpage, the webpage needs to.
     *
     * No parameters
     */
    public void pullInfo() {
        get("/info", (req, res) -> {
            JsonObject obj = createObject("auth-server-ip", "play.greenfieldmc.net");
            JsonArray appArray = new JsonArray();
            JsonObject patreonObj = new JsonObject();
            patreonObj.addProperty("name", "Patreon");
            patreonObj.addProperty("route", "/patreon");
            appArray.add(patreonObj);
            obj.add("apps", appArray);
            return obj;
        });
    }

    /**
     * /authorize
     *
     *  params:
     *      authcode    - The code given to the user for account authorization
     *      uuid        - The UUID of the account being authorized
     */
    public void authorize() {
        post("/authorize", "application/json", (req, res) -> {
            UUID uuid = null;
            try {
                JsonElement body = new JsonParser().parse(req.body());

                //We must be given a json object, if not, fail.
                if (!body.isJsonObject()) throw new RuntimeException();

                JsonObject obj = body.getAsJsonObject();

                //The json element must exist and it must be of primitive type.
                if (!obj.has("authcode") || !obj.has("uuid") || !obj.get("authcode").isJsonPrimitive() || !obj.get("uuid").isJsonPrimitive()) throw new RuntimeException();

                //Ensure the UUID provided is a valid UUID
                try {
                    uuid = UUID.fromString(obj.get("uuid").getAsString());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("UUID Error: Your UUID provided is not properly formatted.");
                }

                //Verify the provided UUID has been validated.
                if (!verificationMap.containsKey(uuid)) throw new RuntimeException();

                //If the auth code provided doesnt match the auth code stored, remove the auth code. user must try again.
                if (!verificationMap.get(uuid).equals(obj.get("authcode").getAsString())) {
                    verificationMap.remove(uuid);
                    throw new RuntimeException("Authorization Error: Your AuthCode did not match your provided auth code.");
                }

                res.header("content-type", "application/json");
                res.status(200);
                //this is where we would return the list of user services.
                return createObject("message", "Success! You are who you say you are.", "status", 200);

                //Otherwise, we want to fail, as the provided auth code didn't match our saved auth code.
            } catch (Exception e) {
                if (uuid != null) verificationMap.remove(uuid);
                res.header("content-type", "application/json");
                res.status(400);
                return createObject("message", e.getMessage() == null ? "Bad request" : e.getMessage(), "status", 400);
            }
        });
    }


    /**
     * /validate
     *
     * params:
     *      uuid    - The users UUID
     */
    public void validate() {
        post("/validate", "application/json", (req, res) -> {
            UUID uuid = null;
            try {
                JsonElement body = new JsonParser().parse(req.body());

                //We must be given a json object, if not, fail.
                if (!body.isJsonObject()) throw new RuntimeException();

                JsonObject obj = body.getAsJsonObject();

                //The json element must exist and it must be of primitive type.
                if (!obj.has("uuid") || !obj.get("uuid").isJsonPrimitive()) throw new RuntimeException();

                //Ensure the UUID provided is a valid UUID
                try {
                    uuid = UUID.fromString(obj.get("uuid").getAsString());
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("UUID Error: Your UUID provided is not properly formatted.");
                }

                JsonElement session = readJsonFromUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replaceAll("-", ""));
                if (session instanceof JsonNull) throw new RuntimeException("Verification Error: There was a problem trying to process your request. Please try again later.");

                res.header("content-type", "application/json");
                res.status(200);
                if (!verificationMap.containsKey(uuid)) verificationMap.put(uuid, RandomStringUtils.random(10, true, true));
                System.out.println("mapping: " + verificationMap.get(uuid));
                return createObject("message", "Success! Please provide your authorization code next.", "status", 200);
            } catch (Exception e) {
                if (uuid != null) verificationMap.remove(uuid);
                res.header("content-type", "application/json");
                res.status(400);
                return createObject("message", e.getMessage() == null ? "Bad request" : e.getMessage(), "status", 400);
            }
        });
    }

//    public void verifyUser() {
//        get("/verify", "application/json", (req, res) -> {
//            res.type("application/json");
//
//        });
//    }

    public JsonObject createObject(Object... values) {
        if (values.length % 2 != 0) throw new RuntimeException("Error creating json object. (key value mismatch)");
        JsonObject obj = new JsonObject();
        for (int i = 0; i < values.length; i+=2) {
            String key = values[i].toString();//this really SHOULD be a string.. im going to treat it that way.
            Object value = values[i + 1];
            if (value instanceof String s) {
                obj.addProperty(key, s);
            } else if (value instanceof Boolean b) {
                obj.addProperty(key, b);
            } else if (value instanceof Number n) {
                obj.addProperty(key, n);
            } else if (value instanceof Character c) {
                obj.addProperty(key, c);
            } else {
                obj.addProperty(key, value.toString());
            }
        }
        return obj;
    }

//    public JsonObject createObject(String key, String val) {
//        JsonObject obj = new JsonObject();
//        obj.addProperty(key, val);
//        return obj;
//    }

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
