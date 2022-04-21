package com.njdaeger.authenticationhub.patreon;

import com.google.gson.JsonObject;
import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.RequestException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.Yaml;
import spark.Request;
import spark.Route;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static com.njdaeger.authenticationhub.web.WebUtils.*;

public class PatreonApplication extends Application {

    private final String clientId;
    private final int requiredPledge;
    private final String clientSecret;

    public PatreonApplication() {
        super();
        Configuration config = getAppConfig();
        if (!config.contains("clientId")) config.set("clientId", "");
        if (!config.contains("clientSecret")) config.set("clientSecret", "");
        if (!config.contains("requiredPledge")) config.set("requiredPledge", -1);
        try {
            ((YamlConfiguration)config).save(appConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.clientId = config.getString("clientId", "");
        this.clientSecret = config.getString("clientSecret", "");
        this.requiredPledge = config.getInt("requiredPledge", -1);


        if (clientId.isEmpty() || requiredPledge < 0 || clientSecret.isEmpty()) {
            Bukkit.getLogger().warning("clientId, clientSecret, or requiredPledge not specified. Unable to start Patreon application.");
        } else if (canBeLoaded) {
            Bukkit.getLogger().info("Loaded Patreon application.");
        }
    }

    @Override
    public String getApplicationName() {
        return "Patreon";
    }

    @Override
    public String getUniqueName() {
        return "patreon";
    }

    @Override
    public String getConnectionUrl(AuthSession session) {
        return "www.patreon.com/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode("http://127.0.0.1:4567/callback") +
                "&state=" + session.getEncodedState(this);
    }

    @Override
    public void handleCallback(Request req, UUID userId, AuthSession session) throws IOException, InterruptedException {
//        try {
            String code = req.queryParamsSafe("code");
            String state = req.queryParams("state");
            if (code == null || state == null) throw new RequestException("App Error: Patreon response was not in the correct format.");
//                if (!token.equals(session.getAuthToken())) throw new RequestException("Authorization Error: Your AuthCode did not match your provided auth code.", UNAUTHORIZED);

            HttpClient client = HttpClient.newHttpClient();
            String reqBody = "?code=" + code + "&grant_type=authorization_code&client_id=" + clientId + "&client_secret=" + clientSecret + "&redirect_uri=" + URLEncoder.encode("http://127.0.0.1:4567/callback");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.patreon.com/api/oauth2/token" + reqBody))
                    .POST(HttpRequest.BodyPublishers.ofString(""))
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
//            JsonObject tokenResp = parseBody(res.body());
//            res.status(OK);
//            res.body(createObject("state", state));
//                res.redirect("http://127.0.0.1:4567/users/" + userId + "?session=" + token, 302);
//            return createObject("state", state);

//        } catch (RequestException e) {
//            res.header("content-type", "application/json");
//            res.status(e.getStatus());
//            return createObject("message", e.getMessage(), "status", e.getStatus());
//        } catch (Exception e) {
//            res.header("content-type", "application/json");
//            res.status(SERVER_ERROR);
//            e.printStackTrace();
//            return createObject("message", "Internal Server Error. Please report this to a system administrator.", "status", SERVER_ERROR);
//        }
    }

//
//    //    @Override
//    public JsonObject connect(UUID userId) throws IOException, InterruptedException {
//        var uri = URI.create("www.patreon.com/oauth2/authorize?" +
//                "response_type=code" +
//                "&client_id=" + clientId +
//                "&redirect_uri=" + URLEncoder.encode("http://127.0.0.1:4567/users/" + userId + "/app/" + getUniqueName() + "/callback") +
//                "&state=" + );
//        var client = HttpClient.newHttpClient();
//        var request = HttpRequest.newBuilder()
//                .GET()
//                .uri(uri)
//                .header("accept", "application/json")
//                .build();
//        var resp = client.send(request, HttpResponse.BodyHandlers.ofString());
//        return createObject("resp", resp.body(), "status", resp.statusCode());
//
//    }
}
