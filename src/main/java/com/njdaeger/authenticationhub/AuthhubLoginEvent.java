package com.njdaeger.authenticationhub;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Fires when a user login should be handled by registered applications.
 */
public class AuthhubLoginEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final AuthenticationHub plugin;
    private PlayerLoginEvent.Result result;
    private String kickMessage;

    public AuthhubLoginEvent(AuthenticationHub plugin, PlayerLoginEvent.Result result, Player who) {
        super(who);
        this.plugin = plugin;
        this.result = result;
        this.kickMessage = "";
    }

    public AuthenticationHub getPlugin() {
        return plugin;
    }

    public PlayerLoginEvent.Result getResult() {
        return result;
    }

    public void setResult(PlayerLoginEvent.Result result) {
        this.result = result;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public void setKickMessage(String kickMessage) {
        this.kickMessage = kickMessage;
    }

    public void disallow(String message) {
        this.result = PlayerLoginEvent.Result.KICK_OTHER;
        this.kickMessage = message;
    }

    public void allow() {
        this.result = PlayerLoginEvent.Result.ALLOWED;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
