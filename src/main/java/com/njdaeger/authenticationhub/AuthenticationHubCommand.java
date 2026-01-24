package com.njdaeger.authenticationhub;

import com.njdaeger.authenticationhub.web.AuthSession;
import com.njdaeger.authenticationhub.web.WebApplication;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static net.md_5.bungee.api.chat.ClickEvent.Action.COPY_TO_CLIPBOARD;
import static net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND;
import static net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT;

public class AuthenticationHubCommand extends BukkitCommand {

    private final WebApplication webApp;

    protected AuthenticationHubCommand(WebApplication webApp) {
        super("authhub");
        this.description = "Get the single-use authorization token for AuthenticationHub's Minecraft account authenticator.";
        this.usageMessage = "/authhub | /authhub reset [uuid] | /authhub view [uuid]";
        this.webApp = webApp;
    }


    /*

    /authhub -> view the sender's auth token, requires no permission.
    /authhub reset -> resets the sender's auth session, if existing.
    /authhub reset [uuid] -> resets a given user's auth session, requires permission authhub.reset-other
    /authhub view [uuid] -> view a given user's auth token, requires permission authhub.view-other

     */

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (webApp == null) {
            sender.sendMessage(ChatColor.RED + "Please enable the web application to use this command.");
            return true;
        }

        UUID userId;
        boolean isReset;
        AuthSession session;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Command usage: /authhub view [uuid]");
                return true;
            }

            isReset = false;
        }
        else if (args[0].equalsIgnoreCase("view")) isReset = false;
        else if (args[0].equalsIgnoreCase("reset")) isReset = true;
        else {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand '" + args[0] + "'");
            return true;
        }

        if (args.length == 2) {
            if (!sender.hasPermission("authhub." + (isReset ? "reset" : "view") + "-other")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to " + (isReset ? "reset" : "view") + " other user sessions.");
                return true;
            }
            try {
                userId = UUID.fromString(args[1]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "The UUID provided was not formatted correctly.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You must specify a UUID to " + (isReset ? "reset" : "view") + ". /authhub " + (isReset ? "reset" : "view") + " [uuid]");
                return true;
            }
            userId = ((Player) sender).getUniqueId();
        }

        session = webApp.getAuthSession(userId);

        if (session == null) {
            sender.sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.DARK_AQUA + "No web session has been started for that user.");
            return true;
        }

        if (isReset) {
            webApp.removeSession(userId);
            var msg = userId.equals(((sender instanceof Player) ? ((Player) sender).getUniqueId() : null)) ? "Your session was reset." : "User session was reset.";
            sender.sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.DARK_AQUA + msg);
            return true;
        }

        //isView
        if (session.isAuthorized()) {
            var builder = new ComponentBuilder().append("[AuthenticationHub] ").color(ChatColor.BLUE);
            var message = builder.append("The session is already authorized. Would you like to reset the session? ").color(ChatColor.DARK_AQUA)
                    .append("\n[Reset]").color(ChatColor.DARK_AQUA).underlined(true).bold(true)
                    .event(new ClickEvent(RUN_COMMAND, "/authhub reset " + userId))
                    .event(new HoverEvent(SHOW_TEXT, new Text(new ComponentBuilder().append("Reset session").color(ChatColor.GRAY).create())))
                    .create();
            if (sender instanceof Player) {
                ((Player)sender).spigot().sendMessage(message);
            } else {
                sender.sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.DARK_AQUA + "The session is already authorized. Please reset the session if you wish to generate a new token.");
            }
            return true;
        }

        var authToken = session.getAuthToken() == null
                ? RandomStringUtils.random(5, true, true).toUpperCase(Locale.ROOT)
                : session.getAuthToken();
        if (session.getAuthToken() == null) {
            session.setAuthToken(authToken);
        }
        var builder = new ComponentBuilder().append("[AuthenticationHub] ").color(ChatColor.BLUE);
        var message = builder.append("Authentication token: ").color(ChatColor.DARK_AQUA)
                .append("\n[Click to Copy]").underlined(true).bold(true)
                .event(new ClickEvent(COPY_TO_CLIPBOARD, session.getAuthToken()))
                .event(new HoverEvent(SHOW_TEXT, new Text(new ComponentBuilder().append("Copy the auth token").color(ChatColor.GRAY).create())))
                .append(" or ").retain(ComponentBuilder.FormatRetention.NONE).color(ChatColor.DARK_AQUA)
                .append("\n[Hover to View]").underlined(true).bold(true)
                .event(new HoverEvent(SHOW_TEXT, new Text(new ComponentBuilder().append(session.getAuthToken()).color(ChatColor.GRAY).create()))).create();

        if (sender instanceof Player) {
            ((Player) sender).spigot().sendMessage(message);
        } else {
            sender.sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.DARK_AQUA + "Authentication token: " + ChatColor.UNDERLINE + ChatColor.DARK_AQUA + session.getAuthToken());
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        if (webApp == null) return List.of();
        if (args.length == 1) return List.of("reset", "view");
        if ((args[0].equalsIgnoreCase("reset") && sender.hasPermission("authhub.reset-other")) ||
                (args[0].equalsIgnoreCase("view") && sender.hasPermission("authhub.view-other"))) {
            if (args.length == 2) return webApp.getActiveSessionIds().stream().map(UUID::toString).toList();
        }
        return List.of();
    }
}
