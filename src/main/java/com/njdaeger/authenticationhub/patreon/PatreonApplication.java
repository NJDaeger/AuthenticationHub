package com.njdaeger.authenticationhub.patreon;

import com.njdaeger.authenticationhub.Application;
import com.njdaeger.authenticationhub.AuthenticationHub;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;

public class PatreonApplication extends Application {

    private final String apiToken;
    private final int requiredPledge;

    public PatreonApplication() {
        super();
        Configuration config = getAppConfig();
        this.apiToken = config.getString("apiToken");
        this.requiredPledge = config.getInt("requiredPledge", -1);

        if (apiToken == null || requiredPledge < 0) {
            Bukkit.getLogger().warning("apiToken or requiredPledge not specified. Unable to start Patreon application.");
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
    public void connect() {

    }
}
