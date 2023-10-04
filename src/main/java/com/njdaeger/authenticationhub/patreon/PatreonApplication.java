package com.njdaeger.authenticationhub.patreon;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.AuthenticationHub;
import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.RequestException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import spark.Request;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.bukkit.ChatColor.*;

public class PatreonApplication extends Application<PatreonUser> {

    private final String clientId;
//    private final int requiredPledge;
    private final String clientSecret;
    private final String patreonUrl;
    private final UUID campaignOwner;
    private final Set<UUID> currentlyRefreshing;
    private final Set<UUID> gettingPledgeStatus;
    private final Map<UUID, Integer> pledgeStatus;
    private int campaignId;

    public PatreonApplication(Plugin plugin) {
        super(plugin);
        Configuration config = getAppConfig();
        if (!config.contains("clientId")) config.set("clientId", "");
        if (!config.contains("clientSecret")) config.set("clientSecret", "");
//        if (!config.contains("requiredPledge")) config.set("requiredPledge", -1);
        if (!config.contains("campaignOwnerUuid")) config.set("campaignOwnerUuid", "");
        if (!config.contains("patreonUrl")) config.set("patreonUrl", "");

        if (!config.contains("messages.expiredUser")) config.set("messages.expiredUser", RED + "Your Patreon account verification has expired. Please re-verify your account by visiting " + GRAY + UNDERLINE + AuthenticationHub.getInstance().getAuthHubConfig().getHubUrl());
        if (!config.contains("messages.refreshingUserToken")) config.set("messages.refreshingUserToken", DARK_AQUA + "Your Patreon account is currently being re-verified. Please wait a few seconds and try again.");
        if (!config.contains("messages.gettingPledgeStatus")) config.set("messages.gettingPledgeStatus", DARK_AQUA + "We are verifying your Patreon pledge status. Please wait a few seconds and try again.");
        if (!config.contains("messages.notAPatron")) config.set("messages.notAPatron", RED + "You are not whitelisted or an Architect patron on this server.");
//
        try {
            ((YamlConfiguration)config).save(appConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.currentlyRefreshing = ConcurrentHashMap.newKeySet();
        this.gettingPledgeStatus = ConcurrentHashMap.newKeySet();
        this.pledgeStatus = new ConcurrentHashMap<>();
        this.clientId = config.getString("clientId", "");
        this.clientSecret = config.getString("clientSecret", "");
        var campaignOwnerUuid = config.getString("campaignOwnerUuid", "");
        this.campaignOwner = !campaignOwnerUuid.isEmpty() ? UUID.fromString(campaignOwnerUuid) : null;
        this.patreonUrl = config.getString("patreonUrl", "");

        if (clientId.isEmpty() || clientSecret.isEmpty() || patreonUrl.isEmpty() || campaignOwner == null) {
            Bukkit.getLogger().warning("Make sure you have the fields 'clientId', 'clientSecret', 'requiredPledge', 'campaignOwnerUuid', and 'patreonUrl' set in your patreon.yml for the Patreon application to start up.");
            canBeLoaded = false;
            return;
        }
        var owner = getConnection(campaignOwner);
        this.campaignId = resolveCampaignId(owner);
        if (campaignId == 0) {
            Bukkit.getLogger().warning("User " + campaignOwner + " does not own the patreon campaign " + patreonUrl + ", or the campaign does not exist (there may be a typo in the url?).");
            canBeLoaded = false;
            return;
        }
        if (canBeLoaded) {
            if (campaignId == -1) Bukkit.getLogger().warning("Patreon application is almost ready to be used, but the campaign owner must be linked with this application to finish the setup.");
            else {
                if (owner.isAlmostExpired()) {
                    Bukkit.getLogger().warning("Campaign owner connection is almost expired, refreshing.");
                    refreshUserToken(campaignOwner, owner);
                }
                Bukkit.getPluginManager().registerEvents(new PatreonListener(this), plugin);
                Bukkit.getLogger().info("Patreon application loaded. Campaign Owner: " + campaignOwner + ", Campaign ID: " + campaignId);
                Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                    Bukkit.getLogger().info("Checking all Patreon User refresh statuses...");
                    var users = database.getConnections(this);
                    users.forEach((uuid, user) -> {
                        if (user.isAlmostExpired()) refreshUserToken(uuid, user);
                    });
                }, 6000);
            }
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
                "&scope=" + URLEncoder.encode("identity identity.memberships campaigns campaigns.members") +
                "&state=" + session.getEncodedState(this);
    }

    @Override
    public void handleConnectCallback(Request req, UUID userId, AuthSession session) throws IOException, InterruptedException {
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
        var id = resolvePatronId(body.get("access_token").getAsString());
        var pledge = campaignId != -1 ? resolvePatronPledge(getConnection(campaignOwner), id) : 0;
        database.saveUserConnection(this, userId, new PatreonUser(body.get("refresh_token").getAsString(), body.get("access_token").getAsString(), body.get("expires_in").getAsLong() * 1000 + System.currentTimeMillis(), body.get("token_type").getAsString(), body.get("scope").getAsString(), id, pledge));
        Bukkit.getLogger().info("Callback handled for " + userId.toString() + " - pledge is " + pledge + " cents");
        //below is for initial application setup
        if (campaignId == -1 && campaignOwner.equals(userId)) {
            Bukkit.getLogger().info("Completing setup for Patreon application...");
            var owner = getConnection(userId);
            campaignId = resolveCampaignId(owner);
            pledge = resolvePatronPledge(owner, owner.getPatreonUserId());
            owner.updateUserPledge(userId, pledge);
            database.saveUserConnection(this, userId, owner);
            Bukkit.getLogger().info("Patreon application setup is now complete.");
        }
        Bukkit.getLogger().info("Cached pledge status for user " + userId + " is " + pledge + " cents. handleConnectionCallback()");
        pledgeStatus.put(userId, pledge);
    }

    @Override
    public String getDisconnectUrl(AuthSession session) {
        return "/disconnect?state=" + session.getEncodedState(this);
    }

    @Override
    public void handleDisconnectCallback(Request req, UUID userId, AuthSession session) throws RequestException, IOException, InterruptedException {
        String state = req.queryParamsSafe("state");
        if (state == null) throw new RequestException("App Error: Patreon response was not in the correct format.");

        HttpClient client = HttpClient.newHttpClient();
        String reqBody = "token=" + getConnection(userId).getAccessToken() + "&client_id=" + clientId + "&client_secret=" + clientSecret;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.patreon.com/api/oauth2/revoke?" + reqBody))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        var body = new JsonParser().parse(resp.body()).getAsJsonObject();
        System.out.println(body.toString());

        database.removeUserConnection(this, userId);
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

    /**
     * Check if the application is currently trying to refresh the user token.
     * @param user The user to check.
     * @return True if the application is currently trying to refresh the user token.
     */
    public boolean isRefreshingUserToken(UUID user) {
        return currentlyRefreshing.contains(user);
    }

    /**
     * Check if the application is currently trying to resolve the updated pledge amount for the user.
     * @param user The user to check.
     * @return True if the application is currently trying to resolve the updated pledge amount for the user, false otherwise.
     */
    public boolean isGettingPledgeStatus(UUID user) {
        return gettingPledgeStatus.contains(user);
    }

    /**
     * Gets the amount of cents the user has pledged to the campaign from the Patreon API. Runs a bukkit async task to get the pledge amount.
     * @param userId The minecraft UUID of the user to get the pledge info of
     * @param user The PatreonUser object of the user to get the pledge info of
     * @return The amount of cents the user has pledged to the campaign, -1 if the user has not pledged any cents, or 0 if we are searching for the pledge status and cannot confirm their pledge amount yet.
     * NOTE: By default, it will return the cached pledge amount if it is available- it will still run the async task to update the cache
     */
    public int getPledgingAmountAsync(UUID userId, PatreonUser user) {
        if (gettingPledgeStatus.contains(userId) || currentlyRefreshing.contains(userId)) return 0;//we dont know if the user is pledging or not at this point
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            getPledgingAmountSync(userId, user);
        });
        return pledgeStatus.getOrDefault(userId, 0);//return 0 since we dont know if the user is pledging or not
    }

    /**
     * Gets the amount of cents the user has pledged to the campaign from the Patreon API.
     * @param userId The minecraft UUID of the user to get the pledge info of
     * @param user The PatreonUser object of the user to get the pledge info of
     * @return The amount of cents the user has pledged to the campaign. -1 if the user has not pledged any cents.
     */
    public int getPledgingAmountSync(UUID userId, PatreonUser user) {
        if (user == null) {
            Bukkit.getLogger().info("Cached pledge status for user " + userId + " is " + -1 + " cents. getPledgingAmountSync()");
            pledgeStatus.put(userId, -1);
            return -1;
        }
        if (!pledgeStatus.containsKey(userId)) {
            gettingPledgeStatus.add(userId);
            Bukkit.getLogger().info("No pledge status cached for user " + userId + ", fetching from Patreon.");
        } else {
            Bukkit.getLogger().info("Cached pledge for user " + userId + " is " + pledgeStatus.get(userId) + " cents. Updating from Patreon.");
        }
        var amount = resolvePatronPledge(getConnection(campaignOwner), user.getPatreonUserId());
        Bukkit.getLogger().info("Cached pledge status for user " + userId + " is " + amount + " cents. getPledgingAmountSync()");
        pledgeStatus.put(userId, amount);
        if (user.getPledgingAmount() != amount) {
            user.updateUserPledge(userId, amount);
            database.saveUserConnection(this, userId, user);
        }
        gettingPledgeStatus.remove(userId);
        return amount;
    }

    /**
     * Gets the amount of cents the user has pledged to the campaign.
     * @param userId The minecraft UUID of the user to get the pledge info of
     * @return The amount of cents the user has pledged to the campaign, 0 if the user's pledge has not been cached yet, or -1 if the user is not pledged.
     */
    public int getPledgingAmountCached(UUID userId) {
        return pledgeStatus.getOrDefault(userId, 0);
    }

    public boolean isPledgingAmountCached(UUID userId) {
        return pledgeStatus.containsKey(userId);
    }

    public UUID getCampaignOwner() {
        return campaignOwner;
    }

    public void refreshUserToken(UUID userId, PatreonUser user) {
        refreshUserToken(userId, user, (u, p) -> {});
    }

    public void refreshUserToken(UUID userId, PatreonUser user, BiConsumer<UUID, Boolean> onComplete) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getLogger().info("Refreshing Patreon connection for " + userId);
            currentlyRefreshing.add(userId);
            var pledge = resolvePatronPledge(getConnection(campaignOwner), user.getPatreonUserId());
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.patreon.com/api/oauth2/token?grant_type=refresh_token&refresh_token=" + user.getRefreshToken() + "&client_id=" + clientId + "&client_secret=" + clientSecret))
                    .POST(HttpRequest.BodyPublishers.ofString(""))
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                onComplete.accept(userId, false);
                currentlyRefreshing.remove(userId);
                return;
            }
            var body = new JsonParser().parse(response.body()).getAsJsonObject();
            user.updateUser(userId, body.get("refresh_token").getAsString(), body.get("access_token").getAsString(), body.get("expires_in").getAsLong() * 1000 + System.currentTimeMillis(), body.get("token_type").getAsString(), user.getScope(), pledge);
            Bukkit.getLogger().info("Cached pledge status for user " + userId + " is " + pledge + " cents. refreshUserToken()");
            pledgeStatus.put(userId, pledge);
            database.saveUserConnection(this, userId, user);
            plugin.getLogger().info("Refreshed Patreon connection for " + userId);
            currentlyRefreshing.remove(userId);
            onComplete.accept(userId, true);
        });
    }

    private int resolvePatronPledge(PatreonUser owner, int patronUserId) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            var currentUrl = "https://www.patreon.com/api/oauth2/v2/campaigns/" + campaignId + "/members?fields%5Bmember%5D=currently_entitled_amount_cents&include=user";
            do {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(currentUrl))
                        .GET()
                        .setHeader("Authorization", "Bearer " + owner.getAccessToken())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                var body = new JsonParser().parse(response.body()).getAsJsonObject();
                var data = body.get("data").getAsJsonArray();
                //iterate through the data array to find the json object whose relationships.user.data.id section of the json response matches the patreon user id
                //if the user is not found in the data array, check the links.next section repeat the request with the link provided in the links.next section
                //if the user is found in the data array, break the loop and return that user's currently_entitled_amount_cents
                for (JsonElement element : data) {
                    var userData = element.getAsJsonObject().get("relationships").getAsJsonObject().get("user").getAsJsonObject().get("data").getAsJsonObject();
                    if (userData.get("id").getAsInt() == patronUserId) {
                        int amount = element.getAsJsonObject().get("attributes").getAsJsonObject().get("currently_entitled_amount_cents").getAsInt();
                        //if we find them in the map, we know they are currently pledged or have pledged before- if the amount is 0, they are no longer pledged, so map them to -1.
                        return amount == 0 ? -1 : amount;
                    }
                }
                currentUrl = body.has("links") ? body.get("links").getAsJsonObject().get("next").getAsString() : null;
            } while (currentUrl != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int resolvePatronId(String accessToken) throws IOException, InterruptedException {
        //resolve the patron id from the oauth2 current_user endpoint
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.patreon.com/api/oauth2/v2/identity"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var body = new JsonParser().parse(response.body()).getAsJsonObject();
        return body.get("data").getAsJsonObject().get("id").getAsInt();
    }

    private int resolveCampaignId(PatreonUser owner) {
        if (owner == null) return -1;
        CompletableFuture<Integer> pledgeStatus = CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.patreon.com/api/oauth2/api/current_user/campaigns"))
                        .GET()
                        .setHeader("Authorization", "Bearer " + owner.getAccessToken())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                var body = new JsonParser().parse(response.body()).getAsJsonObject();
                if (body.get("errors") != null) {
                    var error = body.get("errors").getAsJsonArray();
                    error.forEach(e -> {
                        var err = e.getAsJsonObject();
                        plugin.getLogger().severe("There was an error when trying to resolve the campaign Id. [" + err.get("code_name") + "... " + err.get("detail") + "]");
                        plugin.getLogger().warning("This could be an issue with the owners access token.");
                    });
                    return 0;
                }
                var data = body.get("data").getAsJsonArray();
                //find the campaign id of the campaign whose url matches the patreonUrl
                for (JsonElement element : data) {
                    var campaign = element.getAsJsonObject();
                    var attributes = campaign.get("attributes").getAsJsonObject();
                    if (attributes.get("url").getAsString().equals(patreonUrl)) {
                        return campaign.get("id").getAsInt();
                    }
                }
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
        pledgeStatus.thenAccept(pledgeStatus::complete);
        return pledgeStatus.join();
    }

}
