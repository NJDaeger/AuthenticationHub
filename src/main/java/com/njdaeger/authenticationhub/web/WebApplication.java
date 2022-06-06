package com.njdaeger.authenticationhub.web;

import com.google.gson.*;
import com.njdaeger.authenticationhub.ApplicationRegistry;
import com.njdaeger.authenticationhub.AuthenticationHub;
import com.njdaeger.authenticationhub.AuthenticationHubConfig;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import spark.Request;
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
//        BasicConfigurator.configure();
        BasicConfigurator.configure();
        try {

            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("/log4j.properties"));
            PropertyConfigurator.configure(props);
        } catch (IOException e) {
            e.printStackTrace();
        }

        staticFiles.externalLocation(index.getParentFile().getAbsolutePath());
        this.webService = ignite();

        initExceptionHandler((e) -> {
            plugin.getLogger().warning("Webserver initialization failed");
            e.printStackTrace();
        });

        //Initialize endpoints
        index();
        postValidate();
        getInfo();
        postAuthorize();
        getCallback();
        getApplications();
    }

    /**
     * Index
     */
    public void index() {
        get("/", (req, res) -> Files.readString(index.toPath()));
    }

    /**
     * Endpoint to provide basic server information to the index webpage when loaded.
     */
    public void getInfo() {
        get("/info", (req, res) -> createObject("auth_server_ip", config.getAuthServerIp()));
    }

    /**
     * Endpoint that gets a list of applications the user can register with
     */
    public void getApplications() {
        get("/applications", (req, res) -> {
            try {
                var state = getState(req);

                //If the user has not been registered to the database, we dont want to do anything else.
                if (plugin.getDatabase().getUserId(state.uuid()) == -1)
                    throw new RequestException("Session Error: User has not been registered.", UNAUTHORIZED);

                var session = getAuthSessionSafe(state.uuid());

                //If the user is trying to join from another location than what their auth session remembered, they will not be allowed to do this
                if (!session.getIpAddress().equals(req.ip()) || !session.getIpAddress().equals(state.ip()) || !session.isAuthorized())
                    throw new RequestException("Session Error: You are not authorized to do that.", UNAUTHORIZED);

                //If the user does not have an auth token at this point, they have likely not been verified yet. Normally shouldn't happen.
                if (session.getAuthToken() == null)
                    throw new RequestException("Session Error: You do not have an AuthCode.", UNAUTHORIZED);

                //If the auth codes do not match from the encoded state and the session, bad.
                if (!session.getAuthToken().equals(state.authCode()))
                    throw new RequestException("Session Error: Your AuthCode did not match your provided auth code.", UNAUTHORIZED);

                var result = new JsonArray();
                registry.getApplications().forEach(application -> result.add(createObject("name", application.getApplicationName(), "connection", !application.hasConnection(state.uuid()) ? application.getConnectionUrl(session) : null)));
                return createObject("apps", result, "status", OK);
            } catch (RequestException e) {
                res.header("content-type", "application/json");
                res.status(e.getStatus());
                return createObject("message", e.getMessage(), "status", e.getStatus());
            } catch (Exception e) {
                res.header("content-type", "application/json");
                res.status(SERVER_ERROR);
                res.redirect("/");
                e.printStackTrace();
                return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
            }
        });
    }

    private void getCallback() {
        get("/callback", (req, res) -> {
            try {
                var state = getState(req);

                //We must ensure that the callback state has an application defined.
                if (state.application() == null)
                    throw new RequestException("Session Error: No application provided to call back to.");

                //If the user has not been registered to the database, we dont want to do anything else.
                if (plugin.getDatabase().getUserId(state.uuid()) == -1)
                    throw new RequestException("Session Error: User has not been registered.", UNAUTHORIZED);

                var session = getAuthSessionSafe(state.uuid());

                //If the user is trying to join from another location than what their auth session remembered, they will not be allowed to do this
                if (!session.getIpAddress().equals(req.ip()) || !session.getIpAddress().equals(state.ip()) || !session.isAuthorized())
                    throw new RequestException("Session Error: You are not authorized to do that.", UNAUTHORIZED);

                //If the user does not have an auth token at this point, they have likely not been verified yet. Normally shouldn't happen.
                if (session.getAuthToken() == null)
                    throw new RequestException("Session Error: You do not have an AuthCode.", UNAUTHORIZED);

                //If the auth codes do not match from the encoded state and the session, bad.
                if (!session.getAuthToken().equals(state.authCode()))
                    throw new RequestException("Session Error: Your AuthCode did not match your provided auth code.", UNAUTHORIZED);

                var application = registry.getApplications().stream().filter(app -> app.getUniqueName().equals(state.application())).findFirst();
                if (application.isPresent()) application.get().handleCallback(req, state.uuid(), session);
                else throw new RequestException("State Error: Bad encoded application.");

                //Redirect them to the main page with the state having no application defined.
                res.redirect("/?state=" + session.getEncodedState(null));
                return null;
            } catch (RequestException e) {
                res.header("content-type", "application/json");
                res.status(e.getStatus());
                return createObject("message", e.getMessage(), "status", e.getStatus());
            } catch (Exception e) {
                res.header("content-type", "application/json");
                res.status(SERVER_ERROR);
                res.redirect("/");
                e.printStackTrace();
                return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
            }
        });
    }

    /**
     * /authorize
     *
     *  params:
     *      authcode    - The code given to the user for account authorization
     *      uuid        - The UUID of the account being authorized
     */
    public void postAuthorize() {
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

                AuthSession session = getAuthSessionSafe(uuid);

                //If the user is trying to join from another location than what their auth session remembered or they are already authorized, they will not be allowed to do this
                if (!session.getIpAddress().equals(req.ip()) || session.isAuthorized())
                    throw new RequestException("Session Error: You are not authorized to do that.", UNAUTHORIZED);

                //If the user does not have an auth token at this point, they have likely not been verified yet. Normally shouldn't happen.
                if (session.getAuthToken() == null)
                    throw new RequestException("Session Error: You do not have an AuthCode.", UNAUTHORIZED);

                //If the auth code provided in this does not match the provided auth code, they will not be allowed to do this.
                if (!session.getAuthToken().equals(body.get("authCode").getAsString()))
                    throw new RequestException("Session Error: Your AuthCode did not match your provided auth code.", UNAUTHORIZED);

                session.setAuthorized(true);
                plugin.getDatabase().createUser(uuid);
                res.status(OK);
                res.header("content-type", "application/json");
                return createObject("call", "/applications?state=" + session.getEncodedState(null), "message", "Authorized!", "status", OK);
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

    /**
     * /validate
     *
     * params:
     *      username    - The users username
     */
    public void postValidate() {
        post("/validate", "application/json", (req, res) -> {
            UUID uuid = null;
            try {
                JsonObject body = parseBody(req);

                //The json element must exist and it must be of primitive type.
                if (!body.has("username") || !body.get("username").isJsonPrimitive()) throw new RequestException();
                var username = body.get("username").getAsString();
                //Ensure the UUID provided is a valid UUID
//                try {
//                    uuid = UUID.fromString(body.get("uuid").getAsString());
//                } catch (IllegalArgumentException e) {
//                    throw new RequestException("UUID Error: Your UUID provided is not properly formatted.");
//                }

                JsonElement session = readJsonFromUrl("https://api.mojang.com/users/profiles/minecraft/" + username);
                if (session instanceof JsonNull || session == null) throw new RequestException("Verification Error: There was a problem trying to process your request. Please try again later.");

                try {
                    uuid = UUID.fromString(session.getAsJsonObject().get("id").getAsString().replaceAll("(.{8})(.{4})(.{4})(.{4})(.+)", "$1-$2-$3-$4-$5"));
                } catch (IllegalArgumentException e) {
                    throw new RequestException("Verification Error: There was a problem trying to process your request. Please try again later.");
                }

                res.header("content-type", "application/json");
                res.status(OK);
                if (!verificationMap.containsKey(uuid)) verificationMap.put(uuid, new AuthSession(uuid, req.ip(), plugin));
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

    /**
     * Gets a list of active session IDs
     * @return List of session UUIDs.
     */
    public List<UUID> getActiveSessionIds() {
        return verificationMap.keySet().stream().toList();
    }

    public Service getWebService() {
        return webService;
    }

    /**
     * Gets the state query parameter from a Request and deserializes it.
     * @param req The request that contains the state parameter.
     * @return The DeserializedState object.
     * @throws RequestException If the user is not authorized for an action, if the state was missing, or if the state was not formatted properly.
     */
    private DeserializedState getState(Request req) throws RequestException {
        if (req.queryParamsSafe("state") == null) throw new RequestException();


        String[] result;
        try {
            result = new String(Base64.getUrlDecoder().decode(req.queryParamsSafe("state").getBytes())).split("\\|");
        } catch (IllegalArgumentException e) {
            throw new RequestException();
        }

        UUID uuid;
        String app = null;
        var ip = result[0];
        var userId = result[1];
        var authCode = result[2];
        if (result.length > 3) app = result[3];

        try {
            uuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new RequestException("Session Error: Bad UUID format.");
        }

        return new DeserializedState(app, ip, uuid, authCode);
    }

    /**
     * Gets an AuthSession and checks whether the auth session exists or if they have timed out.
     * @param uuid The UUID of the user to get the AuthSession of.
     * @return The AuthSession the given UUID corresponds to.
     * @throws RequestException If the UUID has not been verified or if the session has passed its timeout time.
     */
    private AuthSession getAuthSessionSafe(UUID uuid) throws RequestException {

        //If the session map does not contain the given UUID, dont allow this to be called
        if (!verificationMap.containsKey(uuid))
            throw new RequestException("Session Error: Your UUID has not been verified.", UNAUTHORIZED);

        AuthSession session = verificationMap.get(uuid);

        //If the user has timed their session out, the session must be removed from the session map.
        if (session.getTimeRemaining() <=0 ) {
            verificationMap.remove(uuid);
            throw new RequestException("Session Error: You have exceeded the session timeout limit.", FORBIDDEN);
        }

        return session;
    }

    private JsonElement readJsonFromUrl(String url) {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            return new JsonParser().parse(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
