package com.njdaeger.authenticationhub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ConnectionRequirement {


    static {
        CONNECTION_REQUIREMENTS = new HashMap<>();
    }

    /**
     * Required for nobody
     */
    public static final ConnectionRequirement NONE = new ConnectionRequirement("NONE", (p) -> false);

    /**
     * Required for whitelisted members only
     */
    public static final ConnectionRequirement WHITELISTED_ONLY = new ConnectionRequirement("WHITELISTED_ONLY", OfflinePlayer::isWhitelisted);

    /**
     * Required for non-whitelisted members only
     */
    public static final ConnectionRequirement NON_WHITELISTED_ONLY = new ConnectionRequirement("NON_WHITELISTED_ONLY", (p) -> !p.isWhitelisted());

    /**
     * Required for everyone
     */
    public static final ConnectionRequirement ALL = new ConnectionRequirement("ALL", (p) -> true);

    private static final Map<String, ConnectionRequirement> CONNECTION_REQUIREMENTS;
    private final Predicate<Player> required;
    private final String requirementName;

    /**
     * Represents a connection requirement. When a player joins, a connection requirement test will be ran against the user to determine if the user is required to connect to a specific application before proceeding.
     * @param requirementName The name of this connection requirement
     * @param requiredFunction The requirement check function to run on the player when they join the server
     */
    public ConnectionRequirement(String requirementName, Predicate<Player> requiredFunction) {
        if (requirementName == null || requiredFunction == null) throw new RuntimeException("ConnectionRequirement must have a name and a predicate function specified.");
        this.required = requiredFunction;
        this.requirementName = requirementName;

        if (CONNECTION_REQUIREMENTS.containsKey(requirementName)) Bukkit.getLogger().warning("ConnectionRequirement [" + requirementName + "] already registered, value is being overridden.");
        CONNECTION_REQUIREMENTS.put(requirementName, this);
    }

    /**
     * Check whether the player is required to connect based on this connection requirement predicate.
     * @param player The player to test
     * @return True if the player is required to have a connection, false otherwise.
     */
    public boolean isRequired(Player player) {
        return required.test(player);
    }

    /**
     * Get the name of this connection requirement
     * @return The name of this connection requirement
     */
    public String getRequirementName() {
        return requirementName;
    }

    /**
     * Get a connection requirement by name
     * @param requirementName The name of the connection requirement to get
     * @return The found ConnectionRequirement, or null if not found
     */
    public static ConnectionRequirement getRequirement(String requirementName) {
        return CONNECTION_REQUIREMENTS.get(requirementName);
    }

    /**
     * Get a connection requirement by name or default to another requirement function.
     * @param requirementName The name of the connection requirement to get
     * @param def The default value to return if the connection requirement wasn't found
     * @return The found ConnectionRequirement, or default if not found
     */
    public static ConnectionRequirement getRequirementOrDefault(String requirementName, ConnectionRequirement def) {
        return CONNECTION_REQUIREMENTS.getOrDefault(requirementName, def);
    }

}