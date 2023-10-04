package com.njdaeger.authenticationhub.patreon;

import com.njdaeger.authenticationhub.AuthhubLoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PatreonListener implements Listener {

    private final PatreonApplication application;

    public PatreonListener(PatreonApplication application) {
        this.application = application;
    }

    @EventHandler
    public void onPlayerLogin(AuthhubLoginEvent e) {
        var user = application.getConnection(e.getPlayer().getUniqueId());
        var conReq = application.getConnectionRequirement();
        var pule = new PatreonUserLoginEvent(e.getPlugin(), application, user, e.getResult(), e.getPlayer());

        if (conReq.isRequired(e.getPlayer())) {
            Bukkit.getLogger().info("Connection is required for " + e.getPlayer().getName());
            if (user == null) {
                e.disallow(application.getAppConfig().getString("messages.notAPatron", "null"));
                return;
            } else if (user.isExpired()) {
                e.disallow(application.getAppConfig().getString("messages.expiredUser", "null"));
                application.removeConnection(e.getPlayer().getUniqueId());
                return;
            } else if (user.isAlmostExpired()) {
                Bukkit.getLogger().info(user.getTimeUntilExpiration());
                application.refreshUserToken(e.getPlayer().getUniqueId(), user);
            } else application.getPledgingAmountAsync(e.getPlayer().getUniqueId(), user);

            if (application.isRefreshingUserToken(e.getPlayer().getUniqueId())) {
                e.disallow(application.getAppConfig().getString("messages.refreshingUserToken", "null"));
                return;
            }

            if (application.isGettingPledgeStatus(e.getPlayer().getUniqueId())) {
                e.disallow(application.getAppConfig().getString("messages.gettingPledgeProfile", "null"));
                return;
            }
            Bukkit.getPluginManager().callEvent(pule);
            e.setResult(pule.getResult());
            e.setKickMessage(pule.getKickMessage());

        } else if (user != null) {
            if (user.isExpired()) {
                e.getPlayer().sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.RESET + application.getAppConfig().getString("messages.expiredUser", "null"));
                application.removeConnection(e.getPlayer().getUniqueId());
            } else if (user.isAlmostExpired()) {
                Bukkit.getLogger().info(user.getTimeUntilExpiration());
                application.refreshUserToken(e.getPlayer().getUniqueId(), user);
            } else application.getPledgingAmountAsync(e.getPlayer().getUniqueId(), user);
            Bukkit.getPluginManager().callEvent(pule);
            e.setResult(pule.getResult());
            e.setKickMessage(pule.getKickMessage());
        }
    }
}
