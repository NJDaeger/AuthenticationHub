package com.njdaeger.authenticationhub;

import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.WebApplication;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class AuthenticationHubListeners implements Listener {

    private final WebApplication webApp;

    AuthenticationHubListeners(WebApplication webApp) {
        this.webApp = webApp;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent e) {
        if (e.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            AuthSession session = webApp.getAuthSession(e.getPlayer().getUniqueId());
            if (session != null && !session.isAuthorized()) {
                session.setAuthToken(RandomStringUtils.random(10, true, true));
                e.setKickMessage("Your current auth code is: " + ChatColor.UNDERLINE + ChatColor.DARK_AQUA + session.getAuthToken());
            }
        }
    }

}
