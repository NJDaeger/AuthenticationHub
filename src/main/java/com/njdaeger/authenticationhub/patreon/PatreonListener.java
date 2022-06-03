package com.njdaeger.authenticationhub.patreon;

import com.njdaeger.authenticationhub.AuthenticationHub;
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
    public void onPlayerLogin(PlayerLoginEvent event) {
        PatreonUser user = application.getConnection(event.getPlayer().getUniqueId());
        if (Bukkit.getServer().getWhitelistedPlayers().stream().anyMatch(offlinePlayer -> offlinePlayer.getUniqueId().equals(event.getPlayer().getUniqueId()))) {
            if (user != null) {
                if (user.isExpired()) {
                    event.getPlayer().sendMessage(ChatColor.BLUE + "[AuthenticationHub]" + ChatColor.DARK_AQUA + " Your Patreon account verification has expired. Please re-verify your account by visiting " + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + AuthenticationHub.getInstance().getAuthHubConfig().getHubUrl());
                    application.removeConnection(event.getPlayer().getUniqueId());
                    return;
                }
                if (user.isAlmostExpired()) {
                    Bukkit.getLogger().info(user.getTimeUntilExpiration());
                    application.refreshUserToken(event.getPlayer().getUniqueId(), user);
                }
            }
            return;
        }

        if (user == null) return;

        if (user.isExpired()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,  ChatColor.RED + "Your Patreon account verification has expired. Please re-verify your account by visiting " + ChatColor.DARK_AQUA + ChatColor.UNDERLINE + AuthenticationHub.getInstance().getAuthHubConfig().getHubUrl());
            application.removeConnection(event.getPlayer().getUniqueId());
            return;
        }

        if (application.isRefreshingUserToken(event.getPlayer().getUniqueId())) {
            //todo: add "refreshing token" message
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.DARK_AQUA + "Your Patreon account is currently being re-verified. Please wait a few seconds and try again.");
            return;
        }

        //in theory, we could use the getPledgedAmount from the user object, but that can be misleading due to it not updating when its queried (eg. it could show as 0 cents pledged
        // the first call, but after they join a second time, it updates to the proper amount since the amount updates upon a user joining the server).
        // This way, we can get the exact pledging amount every time and ensure the user is aware when we are simply just refreshing the cached amount on our end.
        var amount = application.getPledgingAmount(event.getPlayer().getUniqueId(), user);

        if (application.isGettingPledgeStatus(event.getPlayer().getUniqueId()) || amount == 0) {//this means the application is still resolving the pledge status, we dont want to refresh the user token while this is occurring.
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,  ChatColor.DARK_AQUA + "We are verifying your Patreon pledge status. Please wait a few seconds and try again.");
            return;
        }

        if (amount == -1) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "You are not whitelisted or an Architect patron on this server. To apply, visit http://apply.greenfieldmc.net and to become a patron, visit http://patreon.greenfieldmc.net");
        } else if (amount < application.getRequiredPledge()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.DARK_AQUA + "Upgrade your pledging plan to Architect tier to access the server.");
        } else {
            event.allow();
            Bukkit.getLogger().info(event.getPlayer().getName() + " has logged in via Patreon with a pledge of " + amount + " cents.");
        }

        if (user.isAlmostExpired()) {
            Bukkit.getLogger().info(user.getTimeUntilExpiration());
            application.refreshUserToken(event.getPlayer().getUniqueId(), user);
        }
    }

}
