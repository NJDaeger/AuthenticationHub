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
        this.usageMessage = "/authhub [reset] [uuid]";
        this.webApp = webApp;
    }

    //
    // /authhub                                                                 anyone
    // /authhub reset           - resets sender's auth session, if existing.    anyone
    // /authhub reset [uuid]    - resets a given user                           authhub.reset-other
    //
    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        //todo update the command to have better chat formatting and maybe a help command.
        UUID userId;
        boolean reset = false;
        boolean player = sender instanceof Player;

        if (webApp == null) {
            sender.sendMessage(ChatColor.RED + "Please enable the web application to use this command.");
            return true;
        }

        if (args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Please do '/help authhub' for assistance");
            return true;
        }

        //The only subcommand we have available to us is the "reset" subcommand. If the executed command has any
        //arguments, the only one we will be looking for is "reset", if it is anything else, throw an error.
        if (args.length > 0) {
//            if (args[0].equalsIgnoreCase("refresh")) {
//                var app = AuthenticationHub.getInstance().getApplicationRegistry().getApplication(PatreonApplication.class);
//                if (!(sender instanceof Player) || !app.getCampaignOwner().equals(((Player) sender).getUniqueId())) {
//                    sender.sendMessage(ChatColor.RED + "You must be the campaign owner to do that!");
//                    return true;
//                }
//                var user = app.getConnection(((Player) sender).getUniqueId());
//                app.refreshUserToken(((Player) sender).getUniqueId(), user, (u, success) -> {
//                    if (success) sender.sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.DARK_AQUA + "Refreshed your user token!");
//                    else sender.sendMessage(ChatColor.RED + "Failed to refresh your user token. Please see the console for errors.");
//                });
//                sender.sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.DARK_AQUA + "Refreshing your user token...");
//                return true;
//            }
            /*else */if (args[0].equalsIgnoreCase("reset")) reset = true;
            else {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand '" + args[0] + "'");
                return true;
            }
        }

        //If we give 0 or 1 command argument, we have to be a player. consoles MUST specify a UUID
        if (args.length <= 1) {
            if (!player) {
                sender.sendMessage(ChatColor.RED + "You must specify a UUID to reset. /authhub reset [uuid]");
                return true;
            }
            userId = ((Player) sender).getUniqueId();
        } else {//If we have more than one argument, we are definitely trying to reset another user's session.
            //In this case, if we are a player, we need to check if we have permission to reset another user.
            if (player && !sender.hasPermission("authhub.reset-other")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reset other user sessions.");
                return true;
            }
            //Otherwise, if we are anything but a player, we probably have permission, so we allow it
            try {
                userId = UUID.fromString(args[1]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "The UUID provided was not formatted correctly.");
                return true;
            }
        }

        AuthSession session = webApp.getAuthSession(userId);
        var builder = new ComponentBuilder().append("[AuthenticationHub] ").color(ChatColor.BLUE);

        //If the given session is null, just fail gracefully
        if (session == null) {
            sender.sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.DARK_AQUA + "No web session has been started for that user.");
            return true;
        }

        //If we are resetting the session, remove it from the webapp session map
        if (reset) {
            var msg = player && ((Player) sender).getUniqueId().equals(userId) ? "Your session was reset." : "User session was reset.";
            sender.sendMessage(ChatColor.BLUE + "[AuthenticationHub] " + ChatColor.DARK_AQUA + msg);
            webApp.removeSession(userId);
            return true;
        }

        //If the session is authorized, ask the user if they want to reset their session instead.
        if (session.isAuthorized()) {
            var message = builder.append("Your session is already authorized. Would you like to reset your session? ").color(ChatColor.DARK_AQUA)
                    .append("\n[Reset]").color(ChatColor.DARK_AQUA).underlined(true).bold(true)
                    .event(new ClickEvent(RUN_COMMAND, "/authhub reset"))
                    .event(new HoverEvent(SHOW_TEXT, new Text(new ComponentBuilder().append("Reset session").color(ChatColor.GRAY).create())))
                    .create();
            ((Player)sender).spigot().sendMessage(message);
            return true;
        }

        //Otherwise, we are just generating a new token.
        session.setAuthToken(RandomStringUtils.random(10, true, true).toUpperCase(Locale.ROOT));
        var message = builder.append("New authentication token generated! ").color(ChatColor.DARK_AQUA)
                .append("\n[Click to Copy]").underlined(true).bold(true)
                .event(new ClickEvent(COPY_TO_CLIPBOARD, session.getAuthToken()))
                .event(new HoverEvent(SHOW_TEXT, new Text(new ComponentBuilder().append("Copy your auth token").color(ChatColor.GRAY).create())))
                .append(" or ").retain(ComponentBuilder.FormatRetention.NONE).color(ChatColor.DARK_AQUA)
                .append("[Hover to View]").underlined(true).bold(true)
                .event(new HoverEvent(SHOW_TEXT, new Text(new ComponentBuilder().append(session.getAuthToken()).color(ChatColor.GRAY).create()))).create();
        ((Player) sender).spigot().sendMessage(message);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        if (webApp == null) return List.of();
        if (args.length == 1) return List.of("reset");
        if (args[0].equalsIgnoreCase("reset") && sender.hasPermission("authhub.reset-other") && args.length == 2) return webApp.getActiveSessionIds().stream().map(UUID::toString).toList();
        return List.of();
    }
}
