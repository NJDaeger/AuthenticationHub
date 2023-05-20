package com.njdaeger.authenticationhub.discord;

import com.google.gson.JsonParser;
import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.AuthenticationHub;
import com.njdaeger.authenticationhub.ConnectionRequirement;
import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.RequestException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import spark.Request;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import static org.bukkit.ChatColor.*;
import static org.bukkit.ChatColor.RED;

public class DiscordApplication extends Application<DiscordUser> {

    private final String clientId;
    private final String clientSecret;

    //This is a list of users whose Oauth tokens are being refreshed
    private final Set<UUID> currentlyRefreshing;

    //this is a list of users whose discordUserProfiles are being retrieved currently.
    private final Set<UUID> gettingDiscordUserProfile;

    //this is a map of users and their discord user profiles.
    private final Map<UUID, DiscordUserProfile> userProfiles;

    public DiscordApplication(Plugin plugin) {
        super(plugin);
        CookieHandler.setDefault(new CookieManager());
        Configuration config = getAppConfig();
        if (!config.contains("clientId")) config.set("clientId", "");
        if (!config.contains("clientSecret")) config.set("clientSecret", "");
        if (!config.contains("require-connection-for")) config.set("require-connection-for", ConnectionRequirement.NONE.getRequirementName());
        if (ConnectionRequirement.getRequirement(config.getString("require-connection-for")) == null) {
            plugin.getLogger().warning("No 'require-connection-for' property is set in discord.yml, defaulting to NONE.");
        }
        if (!config.contains("messages.expiredUser")) config.set("messages.expiredUser", RED + "Your Discord account verification has expired. Please re-verify your account by visiting " + GRAY + UNDERLINE + AuthenticationHub.getInstance().getAuthHubConfig().getHubUrl());
        if (!config.contains("messages.refreshingUserToken")) config.set("messages.refreshingUserToken", DARK_AQUA + "Your Discord account is currently being re-verified. Please wait a few seconds and try again.");
        if (!config.contains("messages.notConnected")) config.set("messages.notConnected", RED + "Your Discord account is not linked to your Minecraft account. Before you can join, please verify your account by visiting " + GRAY + UNDERLINE + AuthenticationHub.getInstance().getAuthHubConfig().getHubUrl());
        if (!config.contains("messages.gettingDiscordProfile")) config.set("messages.gettingDiscordProfile", DARK_AQUA + "We are resolving your Discord profile. Please wait a few seconds and try again.");

        try {
            ((YamlConfiguration)config).save(appConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.clientId = config.getString("clientId", "");
        this.clientSecret = config.getString("clientSecret", "");
        this.currentlyRefreshing = ConcurrentHashMap.newKeySet();
        this.userProfiles = new ConcurrentHashMap<>();
        this.gettingDiscordUserProfile = ConcurrentHashMap.newKeySet();

        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            Bukkit.getLogger().warning("Make sure you have the fields 'clientId' and 'clientSecret' set in your discord.yml for the Discord application to start up.");
            canBeLoaded = false;
        }
    }

    @Override
    public String getApplicationName() {
        return "Discord";
    }

    @Override
    public String getUniqueName() {
        return "discord";
    }

    @Override
    public String getConnectionUrl(AuthSession session) {
        return "https://discord.com/oauth2/authorize" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(authHubConfig.getHubUrl() + "callback") +
                "&scope=" + URLEncoder.encode("identify guilds.members.read") +
                "&state=" + session.getEncodedState(this);
    }

    @Override
    public void handleCallback(Request req, UUID userId, AuthSession session) throws RequestException, IOException, InterruptedException {
        String code = req.queryParamsSafe("code");
        String state = req.queryParamsSafe("state");
        if (code == null || state == null) throw new RequestException("App Error: Discord response was not in the correct format.");

        String reqBody = "code=" + code + "&grant_type=authorization_code&client_id=" + clientId + "&client_secret=" + clientSecret + "&redirect_uri=" + URLEncoder.encode(authHubConfig.getHubUrl() + "callback");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://discord.com/api/oauth2/token"))
                .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        var body = new JsonParser().parse(resp.body()).getAsJsonObject();
        var accessToken = body.get("access_token").getAsString();
        var tokenType = body.get("token_type").getAsString();
        var expiresInHowManySeconds = body.get("expires_in").getAsInt();
        var refresh_token = body.get("refresh_token").getAsString();
        var scope = body.get("scope").getAsString();
        var profile = resolveDiscordUserProfile(accessToken);
        if (profile == null) {
            Bukkit.getLogger().warning("Could not find discord user profile. Aborting saving user connection.");
            return;
        }
        database.saveUserConnection(this, userId, new DiscordUser(refresh_token, accessToken, expiresInHowManySeconds* 1000L + System.currentTimeMillis(), tokenType, scope, profile.snowflake(), profile.username(), profile.discriminator()));
        Bukkit.getLogger().info("Discord callback handled for " + userId.toString() + " - discord user is " + profile.decodedUsername() + "#" + profile.discriminator());
        userProfiles.put(userId, profile);

    }

    @Override
    public DiscordUser getConnection(UUID user) {
        if (!hasConnection(user)) return null;
        return database.getUserConnection(this, user);
    }

    @Override
    public Class<DiscordUser> getSavedDataClass() {
        return DiscordUser.class;
    }

    public boolean isRefreshingUserToken(UUID user) {
        return currentlyRefreshing.contains(user);
    }

    public boolean isGettingDiscordUserProfile(UUID user) {
        return gettingDiscordUserProfile.contains(user);
    }

    public DiscordUserProfile getDiscordUserAsync(UUID userId, DiscordUser user) {
        if (isGettingDiscordUserProfile(userId) || isRefreshingUserToken(userId)) return null;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            getDiscordUserSync(userId, user);
        });
        return userProfiles.getOrDefault(userId, null);
    }

    public DiscordUserProfile getDiscordUserSync(UUID userId, DiscordUser user) {
        if (user == null) {
            userProfiles.put(userId, null);
            return null;
        }
        if (!userProfiles.containsKey(userId) || userProfiles.get(userId) == null) {
            gettingDiscordUserProfile.add(userId);
            Bukkit.getLogger().info("No Discord profile cached for user " + userId + ", fetching from Discord.");
        } else {
            var cached = userProfiles.get(userId);
            Bukkit.getLogger().info("Cached Discord profile for user " + userId + " is " + new String(Base64.getDecoder().decode(cached.username()), StandardCharsets.UTF_8) + "#" + cached.discriminator() + " ID: " + cached.snowflake() + ". Updating from Discord.");
        }
        var profile = resolveDiscordUserProfile(getConnection(userId).getAccessToken());
        userProfiles.put(userId, profile);
        if (!profile.equals(user.getDiscordProfile())) {
            user.updateUsernameAndDisc(userId, Base64.getEncoder().encodeToString(profile.username().getBytes(StandardCharsets.UTF_8)), profile.discriminator());
            database.saveUserConnection(this, userId, user);
        }
        gettingDiscordUserProfile.remove(userId);
        return profile;
    }

    public DiscordUserProfile getDiscordUserCached(UUID userId) {
        return userProfiles.get(userId);
    }

    public void refreshUserToken(UUID userId, DiscordUser user) {
        try {
            Base64.getDecoder().decode(user.getDiscordProfile().username());
        } catch (IllegalArgumentException ignored) {
            Bukkit.getLogger().warning("Unable to refresh user token. " + user.getDiscordProfile().username() + " is not a valid base64 string.");
        }
        refreshUserToken(userId, user, (u, d) -> {});
    }

    public void refreshUserToken(UUID userId, DiscordUser user, BiConsumer<UUID, Boolean> onComplete) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("Refreshing Discord connection for " + userId);
                currentlyRefreshing.add(userId);
                String reqBody = "grant_type=refresh_token&refresh_token=" + user.getRefreshToken() + "&client_id=" + clientId + "&client_secret=" + clientSecret;

                HttpClient client = HttpClient.newBuilder().cookieHandler(CookieHandler.getDefault()).build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://discord.com/api/oauth2/token"))
                        .POST(HttpRequest.BodyPublishers.ofString(reqBody))
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
                var accessToken = body.get("access_token").getAsString();
                var tokenType = body.get("token_type").getAsString();
                var expiresInHowManySeconds = body.get("expires_in").getAsInt();
                var refresh_token = body.get("refresh_token").getAsString();
                String username;
                String discrim;
                var profile = resolveDiscordUserProfile(accessToken);
                if (profile == null) {
                    onComplete.accept(userId, false);
                    plugin.getLogger().warning("There was a problem resolving the Discord user profile of: " + userId + "... Using old username and discriminator data.");
                    username = user.getDiscordProfile().username();//should be in base 64
                    discrim = user.getDiscordProfile().discriminator();
                } else {
                    username = profile.username();//should be in base 64
                    discrim = profile.discriminator();
                }
                user.updateUser(userId, refresh_token, accessToken, expiresInHowManySeconds * 1000L + System.currentTimeMillis(), tokenType, user.getScope(), username, discrim);
                database.saveUserConnection(this, userId, user);
                userProfiles.put(userId, user.getDiscordProfile());
                plugin.getLogger().info("Refreshed Discord connection for " + userId);
                currentlyRefreshing.remove(userId);
                onComplete.accept(userId, true);
            } catch (Exception e) {
                e.printStackTrace();
                currentlyRefreshing.remove(userId);
                plugin.getLogger().severe("An error occurred when refreshing Discord connection for " + userId);
            }

        });
    }

    private DiscordUserProfile resolveDiscordUserProfile(String accessToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://discord.com/api/v10/users/@me"))
                    .GET()
                    .header("Authorization", "Bearer " + accessToken)
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            var body = new JsonParser().parse(response.body()).getAsJsonObject();
            return new DiscordUserProfile(body.get("id").getAsString(), Base64.getEncoder().encodeToString( body.get("username").getAsString().getBytes(StandardCharsets.UTF_8)), body.get("discriminator").getAsString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
