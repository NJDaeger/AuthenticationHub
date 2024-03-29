package com.njdaeger.authenticationhub.discord;

import com.njdaeger.authenticationhub.ConnectionRequirement;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class DiscordListener implements Listener {

    private final DiscordApplication application;

    public DiscordListener(DiscordApplication application) {
        this.application = application;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        DiscordUser user = application.getConnection(e.getPlayer().getUniqueId());
        var conReq = ConnectionRequirement.getRequirementOrDefault(application.getAppConfig().getString("require-connection-for"), ConnectionRequirement.NONE);

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
            } else application.getDiscordUserAsync(e.getPlayer().getUniqueId(), user);

            if (application.isRefreshingUserToken(e.getPlayer().getUniqueId())) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, application.getAppConfig().getString("messages.refreshingUserToken", "null"));
                return;
            }

            if (application.isGettingDiscordUserProfile(e.getPlayer().getUniqueId())) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, application.getAppConfig().getString("messages.gettingDiscordProfile", "null"));
                return;
            }

        } else if (user != null) {
            if (user.isExpired()) {
                e.getPlayer().sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.RESET + application.getAppConfig().getString("messages.expiredUser", "null"));
                application.removeConnection(e.getPlayer().getUniqueId());
            } else if (user.isAlmostExpired()) {
                Bukkit.getLogger().info(user.getTimeUntilExpiration());
                application.refreshUserToken(e.getPlayer().getUniqueId(), user);
            } else application.getDiscordUserAsync(e.getPlayer().getUniqueId(), user);
        }
    }

}
