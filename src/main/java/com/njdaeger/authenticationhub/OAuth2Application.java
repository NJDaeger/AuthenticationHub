package com.njdaeger.authenticationhub;

import com.njdaeger.authenticationhub.database.ISavedConnection;
import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.RequestException;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import spark.Request;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public abstract class OAuth2Application<T extends ISavedConnection> extends Application<T>{


    protected final String clientId;

    protected final String clientSecret;
    protected final Configuration config;

    protected final Set<UUID> currentlyRefreshing;

    protected final String appApiUrl;

    protected final String oauthScopes;

    private final String appName;
    private final String uniqueName;
    private final Class<T> savedDataClass;

    //TODO this whole class probably needs to be done
    public OAuth2Application(Plugin plugin, String appName, String uniqueName, String appApiUrl, String oauthScopes, Class<T> savedDataClass) {
        super(plugin);

        this.config = getAppConfig();
        if (!config.contains("clientId")) config.set("clientId", "");
        if (!config.contains("clientSecret")) config.set("clientSecret", "");

        try {
            ((YamlConfiguration)config).save(appConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.currentlyRefreshing = ConcurrentHashMap.newKeySet();
        this.clientId = config.getString("clientId", "");
        this.clientSecret = config.getString("clientSecret", "");
        this.appName = appName;
        this.uniqueName = uniqueName;
        this.appApiUrl = appApiUrl;
        this.oauthScopes = oauthScopes;
        this.savedDataClass = savedDataClass;
    }

    @Override
    public String getApplicationName() {
        return appName;
    }

    @Override
    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public String getConnectionUrl(AuthSession session) {
        return appApiUrl +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(authHubConfig.getHubUrl() + "callback") +
                "&scope=" + URLEncoder.encode(oauthScopes) +
                "&state=" + session.getEncodedState(this);
    }

    @Override
    public void handleCallback(Request req, UUID userId, AuthSession session) throws RequestException, IOException, InterruptedException {

    }

    @Override
    public T getConnection(UUID user) {
        if (!hasConnection(user)) return null;
        return database.getUserConnection(this, user);
    }

    @Override
    public Class<T> getSavedDataClass() {
        return savedDataClass;
    }

    public void refreshUserTokenSync(UUID userId, T user, BiConsumer<UUID, Boolean> onComplete) {
        plugin.getLogger().info("Refreshing " + appName + " OAuth2 connection for " + userId);
        currentlyRefreshing.add(userId);

    }

}
