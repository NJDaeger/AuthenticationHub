package com.njdaeger.authenticationhub;

import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.WebApplication;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.*;

public class AuthenticationHubListeners implements Listener {

    private final WebApplication webApp;
    private final Map<UUID, Long> lastLogin = new HashMap<>();
    private final AuthenticationHub plugin;

    AuthenticationHubListeners(AuthenticationHub plugin, WebApplication webApp) {
        this.webApp = webApp;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent e) {

        var loginEvent = new AuthhubLoginEvent(plugin, e.getResult(), e.getPlayer());

        //only do this if they are currently not allowed into the server, if they are allowed into the server, they should
        //just run the command in game to get their auth code
        if (e.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            if (webApp == null) {
                Bukkit.getPluginManager().callEvent(loginEvent);
                if (loginEvent.getResult() != PlayerLoginEvent.Result.ALLOWED) {
                    e.disallow(loginEvent.getResult(), loginEvent.getKickMessage());
                } else e.allow();
                return;
            }
            AuthSession session = webApp.getAuthSession(e.getPlayer().getUniqueId());
            if (session != null) {
                //if session has been authorized already, let the application handlers take care of the rest of the verification process
                //this is only here to do cleanup of expired sessions
                if (session.isAuthorized()) {
                    if (session.getTimeRemaining() <= 0) {
                        webApp.removeSession(e.getPlayer().getUniqueId());
                        lastLogin.put(e.getPlayer().getUniqueId(), null);
                    }
                } else {
                    //if we arent authorized and have a session open, we want to handle the login process here - dont pass off to applications

                    //if the last login time is null, or the last login time is more than 5 minutes ago, we want to reset the auth code
                    var lastLog = lastLogin.get(e.getPlayer().getUniqueId());
                    if (lastLog == null || System.currentTimeMillis() - lastLog >= 300000) {
                        lastLogin.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
                        session.setAuthToken(RandomStringUtils.random(10, true, true).toUpperCase(Locale.ROOT));
                        e.setKickMessage("Your current auth code is: " + ChatColor.UNDERLINE + ChatColor.DARK_AQUA + session.getAuthToken());
                        return;
                    }

                    //if the session is expired
                    if (session.getTimeRemaining() <= 0) {
                        webApp.removeSession(e.getPlayer().getUniqueId());
                        lastLogin.put(e.getPlayer().getUniqueId(), null);
                        e.setKickMessage("Your session has expired.");

                        // i dont think this will be hit, but putting it here in case it is
                    } else e.setKickMessage("Your current session is active. If you wish to restart your session, please wait " + ChatColor.UNDERLINE + ChatColor.DARK_AQUA + session.getNiceTimeRemaining() + ChatColor.RESET + ". Or, contact a server administrator to manually reset your session.");
                    return;
                }
            }
        }

        Bukkit.getPluginManager().callEvent(loginEvent);
        if (loginEvent.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            e.disallow(loginEvent.getResult(), loginEvent.getKickMessage());
        } else e.allow();
    }

    protected void removeLastLogin(UUID uuid) {
        lastLogin.remove(uuid);
    }

}
