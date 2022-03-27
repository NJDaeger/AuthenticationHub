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
    private final Map<UUID, AuthSession> verificationMap;

    private static final int OK = 200;
    private static final int BAD_REQUEST = 400;
    private static final int UNAUTHORIZED = 401;
    private static final int FORBIDDEN = 403;
    private static final int SERVER_ERROR = 500;

    public WebApplication(Plugin plugin) {
        this.verificationMap = new HashMap<>();
        this.plugin = plugin;
        this.index = new File(plugin.getDataFolder() + File.separator + "web" + File.separator + "index.html");
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
        userRoute();
    }

    public Service getWebService() {
        return webService;
    }

    public void index() {
        get("/", (req, res) -> {
//            MessageDigest digest = MessageDigest.getInstance("SHA-256");
//            String formId = new String(digest.digest(RandomStringUtils.random(20, true, true).getBytes()));
            //The case below should probably never happen, but it is an edge case nonetheless
//            while (verificationMap.containsKey(formId)) formId = new String(digest.digest(RandomStringUtils.random(20, true, true).getBytes()));
//            verificationMap.put(formId, new AuthSession());
            return Files.readString(index.toPath());
        });
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

    public void userRoute() {
        get("/user/:userId", (req, res) -> {
            System.out.println(req.body());
            UUID uuid = null;
            try {
                uuid = UUID.fromString(req.params(":userId"));
            } catch (IllegalArgumentException e) {
                throw new RequestException("UUID Error: Your UUID provided is not properly formatted.");
            }
            if (!verificationMap.containsKey(uuid)) throw new RequestException("Authorization Error: Your UUID has not been verified.", UNAUTHORIZED);
            AuthSession session = verificationMap.get(uuid);

            if (!session.isAuthorized()) throw new RequestException("Authorization Error: You are not authorized for that action.", UNAUTHORIZED);
            verificationMap.remove(uuid);
            return createObject("message", "hello!", "status", OK);
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
                if (!body.isJsonObject()) throw new RequestException();
                JsonObject obj = body.getAsJsonObject();

                //The json element must exist and it must be of primitive type.
                if (!obj.has("authCode") || !obj.has("uuid") || !obj.get("authCode").isJsonPrimitive() || !obj.get("uuid").isJsonPrimitive()) throw new RequestException();

                //Ensure the UUID provided is a valid UUID
                try {
                    uuid = UUID.fromString(obj.get("uuid").getAsString());
                } catch (IllegalArgumentException e) {
                    throw new RequestException("UUID Error: Your UUID provided is not properly formatted.");
                }

                //Verify the provided UUID has been validated.
                if (!verificationMap.containsKey(uuid)) throw new RequestException("Authorization Error: Your UUID has not been verified.", UNAUTHORIZED);
                AuthSession session = verificationMap.get(uuid);

                if (session.getAuthToken() == null) throw new RequestException("Authorization Error: You do not have an AuthCode.", UNAUTHORIZED);

                if (session.isAuthorized()) throw new RequestException("Authorization Error: You must request a new AuthCode.", UNAUTHORIZED);

                if (!session.getAuthToken().equals(obj.get("authCode").getAsString())) throw new RequestException("Authorization Error: Your AuthCode did not match your provided auth code.", UNAUTHORIZED);


                res.header("content-type", "application/json");
                res.status(OK);
                session.setAuthorized(true);
                res.body(createObject("authCode", session.getAuthToken()).toString());
                res.redirect("/user/" + uuid);

                //this is where we would return the list of user services.
                return createObject("message", "Success!11", "status", OK);

                //Otherwise, we want to fail, as the provided auth code didn't match our saved auth code.
            } catch (RequestException e) {
                res.header("content-type", "application/json");
                res.status(e.getStatus());
                return createObject("message", e.getMessage(), "status", e.getStatus());
            } catch (Exception e) {
                if (uuid != null) verificationMap.remove(uuid);
                res.header("content-type", "application/json");
                res.status(SERVER_ERROR);
                e.printStackTrace();
                return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
            }
        });
    }

    /*
    TODO: This list
     - Idea:
        - Create an "instruction" node in the nonce so there can be instructions given to the user from the backend.
        - If the user is online when they put their UUID in, tell them to run /auth in game to get a code
        - If the user is offline, tell them to join and run /auth or join and use the auth code in the disconnect message
        - Routing per UUID
            - eg http://auth.greenfieldmc.net/uuid/
                - if someone attempts to go to a UUID, they will be given a 401 error
     - When a user starts their session, send them a browser cookie, this way no external person can effectively DDOS an individual from authorizing
        - When this user opens the page, they get a cookie with a unique hash in it
        - Every time the user does an action, this cookie is sent back with it for matching
        - If it matches, good
        - If it does not match AND said user session is older than 10 minutes/not closed
     */

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
                if (!body.isJsonObject()) throw new RequestException();
                JsonObject obj = body.getAsJsonObject();

                //The json element must exist and it must be of primitive type.
                if (!obj.has("uuid") || !obj.get("uuid").isJsonPrimitive()) throw new RequestException();

                //Ensure the UUID provided is a valid UUID
                try {
                    uuid = UUID.fromString(obj.get("uuid").getAsString());
                } catch (IllegalArgumentException e) {
                    throw new RequestException("UUID Error: Your UUID provided is not properly formatted.");
                }

                JsonElement session = readJsonFromUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replaceAll("-", ""));
                if (session instanceof JsonNull) throw new RequestException("Verification Error: There was a problem trying to process your request. Please try again later.");

                res.header("content-type", "application/json");
                res.status(OK);
                if (!verificationMap.containsKey(uuid)) verificationMap.put(uuid, new AuthSession(uuid));
                return createObject("message", "Success! Please provide your authorization code next.", "status", OK);
            } catch (RequestException e) {
                if (uuid != null) verificationMap.remove(uuid);
                res.header("content-type", "application/json");
                res.status(e.getStatus());
                return createObject("message", e.getMessage(), "status", e.getStatus());
            } catch (Exception e) {
                if (uuid != null) verificationMap.remove(uuid);
                res.header("content-type", "application/json");
                res.status(SERVER_ERROR);
                e.printStackTrace();
                return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
            }
        });
    }

    public JsonObject createObject(Object... values) {
        if (values.length % 2 != 0) throw new RuntimeException("Error creating json object. (key value mismatch)");
        JsonObject obj = new JsonObject();
        for (int i = 0; i < values.length; i+=2) {
            String key = values[i].toString();//this really SHOULD be a string- im going to treat it that way.
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

    /**
     * Gets an auth session from a given UUID.
     * @param userId The UUID associated with the auth session to get.
     * @return The AuthSession, or null if there was no auth session associated with the given UUID.
     */
    public AuthSession getAuthSession(UUID userId) {
        return verificationMap.get(userId);
    }

    /**
     * Removes a given AuthSession
     * @param userId The UUID associated with the auth session that is to be removed.
     * @return True if the auth session was removed, false if there was no auth session removed.
     */
    public boolean removeSession(UUID userId) {
        return verificationMap.remove(userId) != null;
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
