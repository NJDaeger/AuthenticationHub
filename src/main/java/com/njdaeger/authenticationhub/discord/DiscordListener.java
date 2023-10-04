package com.njdaeger.authenticationhub.discord;

import com.njdaeger.authenticationhub.AuthhubLoginEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DiscordListener implements Listener {

    private final DiscordApplication application;

    public DiscordListener(DiscordApplication application) {
        this.application = application;
    }

    @EventHandler
    public void onPlayerLogin(AuthhubLoginEvent e) {
        var user = application.getConnection(e.getPlayer().getUniqueId());
        var conReq = application.getConnectionRequirement();
        var dule = new DiscordUserLoginEvent(e.getPlugin(), application, user, e.getResult(), e.getPlayer());

        if (conReq.isRequired(e.getPlayer())) {
            if (user == null) {
                e.disallow(application.getAppConfig().getString("messages.notConnected", "null"));
                return;
            } else if (user.isExpired()) {
                e.disallow(application.getAppConfig().getString("messages.expiredUser", "null"));
                application.removeConnection(e.getPlayer().getUniqueId());
                return;
            } else if (user.isAlmostExpired()) {
                Bukkit.getLogger().info(user.getTimeUntilExpiration());
                application.refreshUserToken(e.getPlayer().getUniqueId(), user);
            } else application.getDiscordUserAsync(e.getPlayer().getUniqueId(), user);

            if (application.isRefreshingUserToken(e.getPlayer().getUniqueId())) {
                e.disallow(application.getAppConfig().getString("messages.refreshingUserToken", "null"));
                return;
            }

            if (application.isGettingDiscordUserProfile(e.getPlayer().getUniqueId())) {
                e.disallow(application.getAppConfig().getString("messages.gettingDiscordProfile", "null"));
                return;
            }
            Bukkit.getPluginManager().callEvent(dule);
            e.setResult(dule.getResult());
            e.setKickMessage(dule.getKickMessage());

        } else if (user != null) {
            if (user.isExpired()) {
                e.getPlayer().sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.RESET + application.getAppConfig().getString("messages.expiredUser", "null"));
                application.removeConnection(e.getPlayer().getUniqueId());
            } else if (user.isAlmostExpired()) {
                Bukkit.getLogger().info(user.getTimeUntilExpiration());
                application.refreshUserToken(e.getPlayer().getUniqueId(), user);
            } else application.getDiscordUserAsync(e.getPlayer().getUniqueId(), user);
            Bukkit.getPluginManager().callEvent(dule);
            e.setResult(dule.getResult());
            e.setKickMessage(dule.getKickMessage());
        }
    }

}
