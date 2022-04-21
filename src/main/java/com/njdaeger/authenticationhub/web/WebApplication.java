package com.njdaeger.authenticationhub.web;

import com.google.gson.*;
import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.ApplicationRegistry;
import com.njdaeger.authenticationhub.AuthenticationHub;
import com.njdaeger.authenticationhub.AuthenticationHubConfig;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import spark.Service;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static com.njdaeger.authenticationhub.web.WebUtils.*;
import static spark.Service.ignite;
import static spark.Spark.*;

/**
 *
 */
public class WebApplication {

    private final File index;
    private final Service webService;
    private final AuthenticationHub plugin;
    private final ApplicationRegistry registry;
    private final AuthenticationHubConfig config;
    private final Map<UUID, AuthSession> verificationMap;

    public WebApplication(AuthenticationHub plugin, AuthenticationHubConfig config, ApplicationRegistry registry) {
        this.verificationMap = new HashMap<>();
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
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
        validate();
        pullInfo();
        authorize();
        userRoute();
        appRouting();
        userConnections();
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

    public void userConnections() {
        get("/user/:userid/applications", (req, res) -> {
            UUID uuid = null;
            try {
                if (req.queryParamsSafe("session") == null || req.params(":userId") == null) throw new RequestException();
                try {
                    uuid = UUID.fromString(req.params(":userId"));
                } catch (IllegalArgumentException e) {
                    throw new RequestException("UUID Error: Your UUID provided is not properly formatted.");
                }

                //If the user has not been registered to the database, we dont want to do anything else.
                if (plugin.getDatabase().getUserId(uuid) == -1)
                    throw new RequestException("Authorization Error: User has not been registered.", UNAUTHORIZED);

                //If the session map does not contain the given UUID, dont allow this to be called
                if (!verificationMap.containsKey(uuid))
                    throw new RequestException("Authorization Error: Your UUID has not been verified.", UNAUTHORIZED);

                AuthSession session = verificationMap.get(uuid);

                //If the user has timed their session out, the session must be removed from the session map.
                if ((System.currentTimeMillis() - session.getSessionStart()) > 1000*60*10) {
                    verificationMap.remove(uuid);
                    throw new RequestException("Timeout: You have exceeded the session timeout limit.", FORBIDDEN);
                }

                //If the user is trying to join from another location than what their auth session remembered, they will not be allowed to do this
                if (!session.getIpAddress().equals(req.ip()) || !session.isAuthorized())
                    throw new RequestException("Authorization Error: You are not authorized to do that.", UNAUTHORIZED);

                //If the user does not have an auth token at this point, they have likely not been verified yet. Normally shouldn't happen.
                if (session.getAuthToken() == null)
                    throw new RequestException("Authorization Error: You do not have an AuthCode.", UNAUTHORIZED);

                //If the auth code provided in this does not match the provided auth code, they will not be allowed to do this.
                if (!session.getAuthToken().equals(req.queryParamsSafe("session")))
                    throw new RequestException("Authorization Error: Your AuthCode did not match your provided auth code.", UNAUTHORIZED);

                var result = new JsonArray();
                UUID finalUuid = uuid;
                registry.getApplications().forEach(application -> {
                    result.add(createObject("name", application.getApplicationName(), "connected", application.hasConnection(finalUuid), "connection", application.getConnectionUrl(session)));
                });
                return createObject("apps", result);
            } catch (RequestException e) {
                res.header("content-type", "application/json");
                res.status(e.getStatus());
                return createObject("message", e.getMessage(), "status", e.getStatus());
            } catch (Exception e) {
                if (uuid != null) verificationMap.remove(uuid);
                res.header("content-type", "application/json");
                res.status(SERVER_ERROR);
                res.redirect("/");
                e.printStackTrace();
                return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
            }
        });
    }

    public void userRoute() {
        get("/user/:userId", (req, res) -> {
            UUID uuid = null;
            try {
                if (req.queryParamsSafe("session") == null || req.params(":userId") == null) throw new RequestException();
                try {
                    uuid = UUID.fromString(req.params(":userId"));
                } catch (IllegalArgumentException e) {
                    throw new RequestException("UUID Error: Your UUID provided is not properly formatted.");
                }

                //If the user has not been registered to the database, we dont want to do anything else.
                if (plugin.getDatabase().getUserId(uuid) == -1)
                    throw new RequestException("Authorization Error: User has not been registered.", UNAUTHORIZED);

                //If the session map does not contain the given UUID, dont allow this to be called
                if (!verificationMap.containsKey(uuid))
                    throw new RequestException("Authorization Error: Your UUID has not been verified.", UNAUTHORIZED);

                AuthSession session = verificationMap.get(uuid);

                //If the user has timed their session out, the session must be removed from the session map.
                if ((System.currentTimeMillis() - session.getSessionStart()) > 1000*60*10) {
                    verificationMap.remove(uuid);
                    throw new RequestException("Timeout: You have exceeded the session timeout limit.", FORBIDDEN);
                }

                //If the user is trying to join from another location than what their auth session remembered, they will not be allowed to do this
                if (!session.getIpAddress().equals(req.ip()) || !session.isAuthorized())
                    throw new RequestException("Authorization Error: You are not authorized to do that.", UNAUTHORIZED);

                //If the user does not have an auth token at this point, they have likely not been verified yet. Normally shouldn't happen.
                if (session.getAuthToken() == null)
                    throw new RequestException("Authorization Error: You do not have an AuthCode.", UNAUTHORIZED);

                //If the auth code provided in this does not match the provided auth code, they will not be allowed to do this.
                if (!session.getAuthToken().equals(req.queryParamsSafe("session")))
                    throw new RequestException("Authorization Error: Your AuthCode did not match your provided auth code.", UNAUTHORIZED);

                return Files.readString(index.toPath());
            } catch (RequestException e) {
                res.header("content-type", "application/json");
                res.status(e.getStatus());
                return createObject("message", e.getMessage(), "status", e.getStatus());
            } catch (Exception e) {
                if (uuid != null) verificationMap.remove(uuid);
                res.header("content-type", "application/json");
                res.status(SERVER_ERROR);
                res.redirect("/");
                e.printStackTrace();
                return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
            }
        });
    }

    //todo: ideally, we should make it redirect to just a standard /callback, and have the ?state variable contain ALL information needed to reconstruct the page. eg. the uuid, authcode, application just called back.
    private void appRouting() {
        get("/callback", (req, res) -> {
            UUID uuid = null;

            if (req.queryParamsSafe("state") == null) throw new RequestException();

            try {

                if (req.queryParamsSafe("state") == null) throw new RequestException();

                var result = new String(Base64.getUrlDecoder().decode(req.queryParamsSafe("state").getBytes())).split("\\|");
                var appName = result[0];
                var userId = result[1];
                var authToken = result[2];

                System.out.println(req.ip());
                System.out.println(Arrays.toString(result));

                try {
                    uuid = UUID.fromString(userId);
                } catch (IllegalArgumentException e) {
                    throw new RequestException("State Error: Bad encoded UUID.");
                }

                //If the user has not been registered to the database, we dont want to do anything else.
                if (plugin.getDatabase().getUserId(uuid) == -1)
                    throw new RequestException("Authorization Error: User has not been registered.", UNAUTHORIZED);

                //If the session map does not contain the given UUID, dont allow this to be called
                if (!verificationMap.containsKey(uuid))
                    throw new RequestException("Authorization Error: Your UUID has not been verified.", UNAUTHORIZED);

                AuthSession session = verificationMap.get(uuid);

                //If the user has timed their session out, the session must be removed from the session map.
                if ((System.currentTimeMillis() - session.getSessionStart()) > 1000*60*10) {
                    verificationMap.remove(uuid);
                    throw new RequestException("Timeout: You have exceeded the session timeout limit.", FORBIDDEN);
                }

                //If the user is trying to join from another location than what their auth session remembered, they will not be allowed to do this
                if (!session.getIpAddress().equals(req.ip()) || !session.isAuthorized())
                    throw new RequestException("Authorization Error: You are not authorized to do that.", UNAUTHORIZED);

                //If the user does not have an auth token at this point, they have likely not been verified yet. Normally shouldn't happen.
                if (session.getAuthToken() == null)
                    throw new RequestException("Authorization Error: You do not have an AuthCode.", UNAUTHORIZED);

                if (!session.getAuthToken().equals(authToken))
                    throw new RequestException("Authorization Error: Your AuthCode did not match your provided auth code.", UNAUTHORIZED);

                var application = registry.getApplications().stream().filter(app -> app.getUniqueName().equals(appName)).findFirst();
                if (application.isPresent()) application.get().handleCallback(req, uuid, session);
                else throw new RequestException("State Error: Bad encoded application.");

                res.redirect("/?state=" + req.queryParamsSafe("state"));
                return null;
            } catch (RequestException e) {
                res.header("content-type", "application/json");
                res.status(e.getStatus());
                return createObject("message", e.getMessage(), "status", e.getStatus());
            } catch (Exception e) {
                if (uuid != null) verificationMap.remove(uuid);
                res.header("content-type", "application/json");
                res.status(SERVER_ERROR);
                res.redirect("/");
                e.printStackTrace();
                return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
            }
        });
//        registry.getApplications().forEach(application-> {
//            post("/user/:userId/app/" + application.getUniqueName() + "/callback", (req, res) -> {
//                UUID uuid = null;
//                try {
//
//                    if (req.params(":userId") == null) throw new RequestException();
//
//                    try {
//                        uuid = UUID.fromString(req.params(":userId"));
//                    } catch (IllegalArgumentException e) {
//                        throw new RequestException("UUID Error: Your UUID provided is not properly formatted.");
//                    }
//
//                    //If the user has not been registered to the database, we dont want to do anything else.
//                    if (plugin.getDatabase().getUserId(uuid) == -1)
//                        throw new RequestException("Authorization Error: User has not been registered.", UNAUTHORIZED);
//
//                    //If the session map does not contain the given UUID, dont allow this to be called
//                    if (!verificationMap.containsKey(uuid))
//                        throw new RequestException("Authorization Error: Your UUID has not been verified.", UNAUTHORIZED);
//
//                    AuthSession session = verificationMap.get(uuid);
//
//                    //If the user has timed their session out, the session must be removed from the session map.
//                    if ((System.currentTimeMillis() - session.getSessionStart()) > 1000*60*10) {
//                        verificationMap.remove(uuid);
//                        throw new RequestException("Timeout: You have exceeded the session timeout limit.", FORBIDDEN);
//                    }
//
//                    //If the user is trying to join from another location than what their auth session remembered, they will not be allowed to do this
//                    if (!session.getIpAddress().equals(req.ip()) || !session.isAuthorized())
//                        throw new RequestException("Authorization Error: You are not authorized to do that.", UNAUTHORIZED);
//
//                    //If the user does not have an auth token at this point, they have likely not been verified yet. Normally shouldn't happen.
//                    if (session.getAuthToken() == null)
//                        throw new RequestException("Authorization Error: You do not have an AuthCode.", UNAUTHORIZED);
//
//                    //
//                    //NOTE: AUTH SESSION TOKEN SHOULD BE CHECKED BY THE IMPLEMENTING APPLICATION
//                    //
//                    return application.handleCallback(uuid, session);
//                } catch (RequestException e) {
//                    res.header("content-type", "application/json");
//                    res.status(e.getStatus());
//                    return createObject("message", e.getMessage(), "status", e.getStatus());
//                } catch (Exception e) {
//                    if (uuid != null) verificationMap.remove(uuid);
//                    res.header("content-type", "application/json");
//                    res.status(SERVER_ERROR);
//                    res.redirect("/");
//                    e.printStackTrace();
//                    return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
//                }
//
//            });
//        });
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
                JsonObject body = parseBody(req);

                //The json element must exist and it must be of primitive type.
                if (!body.has("authCode") || !body.has("uuid") || !body.get("authCode").isJsonPrimitive() || !body.get("uuid").isJsonPrimitive())
                    throw new RequestException();

                //Ensure the UUID provided is a valid UUID
                try {
                    uuid = UUID.fromString(body.get("uuid").getAsString());
                } catch (IllegalArgumentException e) {
                    throw new RequestException("UUID Error: Your UUID provided is not properly formatted.");
                }

                //Verify the provided UUID has been validated.
                if (!verificationMap.containsKey(uuid))
                    throw new RequestException("Authorization Error: Your UUID has not been verified.", UNAUTHORIZED);

                AuthSession session = verificationMap.get(uuid);

                //If the user has timed their session out, the session must be removed from the session map.
                if ((System.currentTimeMillis() - session.getSessionStart()) > 1000*60*10) {
                    verificationMap.remove(uuid);
                    throw new RequestException("Timeout: You have exceeded the session timeout limit.", FORBIDDEN);
                }

                //If the user is trying to join from another location than what their auth session remembered or they are already authorized, they will not be allowed to do this
                if (!session.getIpAddress().equals(req.ip()) || session.isAuthorized())
                    throw new RequestException("Authorization Error: You are not authorized to do that.", UNAUTHORIZED);

                //If the user does not have an auth token at this point, they have likely not been verified yet. Normally shouldn't happen.
                if (session.getAuthToken() == null)
                    throw new RequestException("Authorization Error: You do not have an AuthCode.", UNAUTHORIZED);

                //If the auth code provided in this does not match the provided auth code, they will not be allowed to do this.
                if (!session.getAuthToken().equals(body.get("authCode").getAsString()))
                    throw new RequestException("Authorization Error: Your AuthCode did not match your provided auth code.", UNAUTHORIZED);

                //In all other cases, we want to redirect to /user/uuid?session=authtoken
//                res.header("content-type", "application/json");
//                res.status(OK);
                session.setAuthorized(true);
                plugin.getDatabase().createUser(uuid);
//                res.redirect("/user/" + uuid + "?session=" + session.getAuthToken());
                res.header("content-type", "application/json");
                res.status(OK);
                return createObject("call", "/user/" + uuid + "/applications?session=" + session.getAuthToken());
                //this is where we would return the list of user services.
//                return createObject("message", "Success!11", "status", OK);

                //Otherwise, we want to fail, as the provided auth code didn't match our saved auth code.
            } catch (RequestException e) {
                res.header("content-type", "application/json");
                res.status(e.getStatus());
                return createObject("message", e.getMessage(), "status", e.getStatus());
            } catch (Exception e) {
                if (uuid != null) verificationMap.remove(uuid);
                res.header("content-type", "application/json");
                res.status(SERVER_ERROR);
                res.redirect("/");
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
                JsonObject body = parseBody(req);

                //The json element must exist and it must be of primitive type.
                if (!body.has("uuid") || !body.get("uuid").isJsonPrimitive()) throw new RequestException();

                //Ensure the UUID provided is a valid UUID
                try {
                    uuid = UUID.fromString(body.get("uuid").getAsString());
                } catch (IllegalArgumentException e) {
                    throw new RequestException("UUID Error: Your UUID provided is not properly formatted.");
                }

                JsonElement session = readJsonFromUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replaceAll("-", ""));
                if (session instanceof JsonNull) throw new RequestException("Verification Error: There was a problem trying to process your request. Please try again later.");

                res.header("content-type", "application/json");
                res.status(OK);
                if (!verificationMap.containsKey(uuid)) verificationMap.put(uuid, new AuthSession(uuid, req.ip()));
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
                res.redirect("/");
                e.printStackTrace();
                return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
            }
        });
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
