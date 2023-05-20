package com.njdaeger.authenticationhub;

import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.WebApplication;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthenticationHubListeners implements Listener {

    private final WebApplication webApp;
    private final Map<UUID, Long> lastLogin = new HashMap<>();

    AuthenticationHubListeners(WebApplication webApp) {
        this.webApp = webApp;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent e) {
        if (e.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            AuthSession session = webApp.getAuthSession(e.getPlayer().getUniqueId());
            if (session != null) {
                if (session.isAuthorized()) {
                    if (session.getTimeRemaining() <= 0) {
                        webApp.removeSession(e.getPlayer().getUniqueId());
                        lastLogin.put(e.getPlayer().getUniqueId(), null);
                    }
//                    else e.setKickMessage("Your current session is active. If you wish to restart your session, please wait " + ChatColor.UNDERLINE + ChatColor.DARK_AQUA + session.getNiceTimeRemaining() + ChatColor.RESET + ". Or, contact a server administrator to manually reset your session.");
                } else {
                    var lastLog = lastLogin.get(e.getPlayer().getUniqueId());
                    if (lastLog == null) {
                        lastLogin.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
                        session.setAuthToken(RandomStringUtils.random(10, true, true));
                        e.setKickMessage("Your current auth code is: " + ChatColor.UNDERLINE + ChatColor.DARK_AQUA + session.getAuthToken());
                    } else if (System.currentTimeMillis() - lastLog < 10000) e.setKickMessage("Your current auth code is: " + ChatColor.UNDERLINE + ChatColor.DARK_AQUA + session.getAuthToken());
                    else if (session.getTimeRemaining() <= 0) {
                        webApp.removeSession(e.getPlayer().getUniqueId());
                        lastLogin.put(e.getPlayer().getUniqueId(), null);
                    } else e.setKickMessage("Your current session is active. If you wish to restart your session, please wait " + ChatColor.UNDERLINE + ChatColor.DARK_AQUA + session.getNiceTimeRemaining() + ChatColor.RESET + ". Or, contact a server administrator to manually reset your session.");
                }
            }
        }
    }
}
