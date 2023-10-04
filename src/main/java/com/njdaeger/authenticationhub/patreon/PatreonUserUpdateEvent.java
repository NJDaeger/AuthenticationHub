package com.njdaeger.authenticationhub.patreon;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

public class PatreonUserUpdateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final UUID uniqueId;
    private final PatreonUser user;

    public PatreonUserUpdateEvent(UUID userId, PatreonUser user) {
        super(true);
        this.uniqueId = userId;
        this.user = user;
    }

    public PatreonUser getUser() {
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
