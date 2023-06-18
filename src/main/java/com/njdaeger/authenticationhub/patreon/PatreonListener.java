package com.njdaeger.authenticationhub.patreon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PatreonListener implements Listener {

    private final PatreonApplication application;

    public PatreonListener(PatreonApplication application) {
        this.application = application;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        var user = application.getConnection(e.getPlayer().getUniqueId());
        var conReq = application.getConnectionRequirement();

        if (conReq.isRequired(e.getPlayer())) {
            if (user == null) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, application.getAppConfig().getString("messages.notConnected", "null"));
                return;
            } else if (user.isExpired()) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, application.getAppConfig().getString("messages.expiredUser", "null"));
                application.removeConnection(e.getPlayer().getUniqueId());
                return;
            } else if (user.isAlmostExpired()) {
                Bukkit.getLogger().info(user.getTimeUntilExpiration());
                application.refreshUserToken(e.getPlayer().getUniqueId(), user);
                return;
            } else application.getPledgingAmountAsync(e.getPlayer().getUniqueId(), user);

            if (application.isRefreshingUserToken(e.getPlayer().getUniqueId())) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, application.getAppConfig().getString("messages.refreshingUserToken", "null"));
                return;
            }

            if (application.isGettingPledgeStatus(e.getPlayer().getUniqueId())) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, application.getAppConfig().getString("messages.gettingPledgeProfile", "null"));
                return;
            }

        } else if (user != null) {
            if (user.isExpired()) {
                e.getPlayer().sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.RESET + application.getAppConfig().getString("messages.expiredUser", "null"));
                application.removeConnection(e.getPlayer().getUniqueId());
            } else if (user.isAlmostExpired()) {
                Bukkit.getLogger().info(user.getTimeUntilExpiration());
                application.refreshUserToken(e.getPlayer().getUniqueId(), user);
            } else application.getPledgingAmountAsync(e.getPlayer().getUniqueId(), user);
        }
    }
}
