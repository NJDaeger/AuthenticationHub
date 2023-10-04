package com.njdaeger.authenticationhub.discord;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class DiscordUserUpdateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final UUID uniqueId;
    private final DiscordUser user;

    public DiscordUserUpdateEvent(UUID userId, DiscordUser user) {
        super(true);
        this.uniqueId = userId;
        this.user = user;
    }

    public DiscordUser getUser() {
        return user;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
