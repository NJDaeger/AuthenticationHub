package com.njdaeger.authenticationhub.patreon;

import com.google.gson.JsonParser;
import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.RequestException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import spark.Request;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class PatreonApplication extends Application<PatreonUser> {

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
        return "https://www.patreon.com/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(authHubConfig.getHubUrl() + "callback") +
                "&scope=" + URLEncoder.encode("identity identity.memberships campaigns campaigns.members campaigns.members[email] campaigns.members.address") +
                "&state=" + session.getEncodedState(this);
    }

    @Override
    public void handleCallback(Request req, UUID userId, AuthSession session) throws IOException, InterruptedException {
        String code = req.queryParamsSafe("code");
        String state = req.queryParamsSafe("state");
        if (code == null || state == null) throw new RequestException("App Error: Patreon response was not in the correct format.");

        HttpClient client = HttpClient.newHttpClient();
        String reqBody = "?code=" + code + "&grant_type=authorization_code&client_id=" + clientId + "&client_secret=" + clientSecret + "&redirect_uri=" + URLEncoder.encode(authHubConfig.getHubUrl() + "callback");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.patreon.com/api/oauth2/token" + reqBody))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var body = new JsonParser().parse(response.body()).getAsJsonObject();
        database.saveUserConnection(this, userId, new PatreonUser(body.get("refresh_token").getAsString(), body.get("access_token").getAsString(), body.get("expires_in").getAsLong(), body.get("token_type").getAsString(), body.get("scope").getAsString()));
    }

    @Override
    public Class<PatreonUser> getSavedDataClass() {
        return PatreonUser.class;
    }

    @Override
    public PatreonUser getConnection(UUID user) {
        if (!hasConnection(user)) return null;
        return database.getUserConnection(this, user);
    }
}
