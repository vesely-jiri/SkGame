package cz.nox.skgame.core.command;

import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.command.subcommands.AdminSubcommand;
import cz.nox.skgame.core.command.subcommands.JoinSubcommand;
import cz.nox.skgame.core.command.subcommands.SpectateSubcommand;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import cz.nox.skgame.core.gui.services.MainGuiService;
import cz.nox.skgame.core.gui.services.PlayerProfileGuiService;
import cz.nox.skgame.core.gui.services.SessionGuiService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameCommand implements CommandExecutor, TabCompleter {

    private final AdminSubcommand adminSub = new AdminSubcommand();
    private final JoinSubcommand joinSub = new JoinSubcommand();
    private final SpectateSubcommand spectateSub = new SpectateSubcommand();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("/game requires a player sender.");
            return true;
        }
        if (args.length == 0) {
            if (SessionManager.getInstance().getSession(player) != null) {
                SessionGuiService.getInstance().openFor(player);
            } else {
                MainGuiService.getInstance().openFor(player);
            }
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "admin"    -> adminSub.execute(player, args);
            case "join"     -> joinSub.execute(player, args);
            case "spectate" -> spectateSub.execute(player, args);
            case "rejoin"   -> {
                if (args.length < 2) {
                    Messages.send(player, "session.rejoin.usage");
                    return true;
                }
                SessionLifecycleManagerImpl.getInstance().rejoinSession(player, args[1]);
            }
            case "profile"  -> {
                OfflinePlayer subject;
                if (args.length < 2) {
                    if (!player.hasPermission("skgame.profile")) {
                        Messages.send(player, "command.error.no-permission");
                        return true;
                    }
                    subject = player;
                } else {
                    if (!player.hasPermission("skgame.profile.others")) {
                        Messages.send(player, "command.profile.no-permission-others");
                        return true;
                    }
                    Player online = Bukkit.getPlayerExact(args[1]);
                    @SuppressWarnings("deprecation")
                    OfflinePlayer offline = online != null ? online : Bukkit.getOfflinePlayer(args[1]);
                    subject = offline;
                    if (!subject.hasPlayedBefore() && !(subject instanceof Player)) {
                        Messages.send(player, "command.profile.not-found", args[1]);
                        return true;
                    }
                }
                PlayerProfileGuiService.getInstance().openFor(player, subject);
            }
            case "invite"   -> handleInvite(player, args);
            case "uninvite" -> handleUninvite(player, args);
            default         -> Messages.send(player, "command.error.unknown-subcommand");
        }
        return true;
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            Messages.send(player, "command.invite.usage");
            return;
        }
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null || !player.equals(session.getHost())) {
            Messages.send(player, "gui.session.error.not-host");
            return;
        }
        if (session.getVisibility() != SessionVisibility.INVITE_ONLY) {
            Messages.send(player, "command.invite.wrong-mode");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Messages.send(player, "command.profile.not-found", args[1]);
            return;
        }
        session.addInvitedPlayer(target.getUniqueId());
        Messages.send(player, "session.invite.sent", target.getName());
        String joinCmd = "/game join " + session.getId();
        Component msg = LegacyComponentSerializer.legacyAmpersand()
                .deserialize(Messages.get("session.invite.received", target, player.getName()))
                .clickEvent(ClickEvent.runCommand(joinCmd));
        target.sendMessage(msg);
    }

    private void handleUninvite(Player player, String[] args) {
        if (args.length < 2) {
            Messages.send(player, "command.invite.usage");
            return;
        }
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null || !player.equals(session.getHost())) {
            Messages.send(player, "gui.session.error.not-host");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            Messages.send(player, "command.profile.not-found", args[1]);
            return;
        }
        session.removeInvitedPlayer(target.getUniqueId());
        Messages.send(player, "session.invite.revoked", target.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            List<String> opts = new ArrayList<>(List.of("join", "rejoin"));
            if (player.hasPermission("skgame.profile")) opts.add("profile");
            if (player.hasPermission("skgame.spectate")) opts.add("spectate");
            if (player.hasPermission("skgame.admin")) opts.add("admin");
            Session ps = SessionManager.getInstance().getSession(player);
            if (ps != null && player.equals(ps.getHost())) {
                opts.add("invite");
                opts.add("uninvite");
            }
            String partial = args[0].toLowerCase();
            return opts.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "join"     -> joinSub.tabComplete(player, args);
                case "spectate" -> spectateSub.tabComplete(player, args);
                default         -> List.of();
            };
        }
        return List.of();
    }
}
