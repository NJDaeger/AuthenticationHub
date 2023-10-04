package com.njdaeger.authenticationhub.patreon;

import com.njdaeger.authenticationhub.AuthenticationHub;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class PatreonUserLoginEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final PatreonApplication application;
    private final AuthenticationHub plugin;
    private PlayerLoginEvent.Result result;
    private final PatreonUser user;
    private String kickMessage;

    public PatreonUserLoginEvent(AuthenticationHub plugin, PatreonApplication application, PatreonUser user, PlayerLoginEvent.Result result, Player who) {
        super(who);
        this.user = user;
        this.plugin = plugin;
        this.result = result;
        this.kickMessage = "";
        this.application = application;
    }

    public AuthenticationHub getPlugin() {
        return plugin;
    }

    public PatreonApplication getApplication() {
        return application;
    }

    public PatreonUser getUser() {
        return user;
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
