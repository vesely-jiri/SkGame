package cz.nox.skgame.core.command;

import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.command.subcommands.AdminSubcommand;
import cz.nox.skgame.core.command.subcommands.JoinSubcommand;
import cz.nox.skgame.core.command.subcommands.QuickplaySubcommand;
import cz.nox.skgame.core.command.subcommands.SpectateSubcommand;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import cz.nox.skgame.core.gui.services.MainGuiService;
import cz.nox.skgame.core.gui.services.GameHistoryGuiService;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GameCommand implements CommandExecutor, TabCompleter {

    private final AdminSubcommand adminSub = new AdminSubcommand();
    private final JoinSubcommand joinSub = new JoinSubcommand();
    private final QuickplaySubcommand quickplaySub = new QuickplaySubcommand();
    private final SpectateSubcommand spectateSub = new SpectateSubcommand();
    /** Reporter UUID → timestamp of last successful report (ms). Cleared on server restart only. */
    private final Map<UUID, Long> reportCooldowns = new ConcurrentHashMap<>();

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
            case "admin"     -> adminSub.execute(player, args);
            case "join"      -> joinSub.execute(player, args);
            case "quickplay" -> quickplaySub.execute(player, args);
            case "spectate"  -> spectateSub.execute(player, args);
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
            case "history"  -> handleHistory(player, args);
            case "invite"   -> handleInvite(player, args);
            case "uninvite" -> handleUninvite(player, args);
            case "kick"     -> handleKick(player, args);
            case "ban"      -> handleBan(player, args);
            case "unban"    -> handleUnban(player, args);
            case "report"   -> handleReport(player, args);
            default         -> Messages.send(player, "command.error.unknown-subcommand");
        }
        return true;
    }

    private void handleHistory(Player player, String[] args) {
        OfflinePlayer subject;
        if (args.length < 2) {
            subject = player;
        } else {
            if (!player.hasPermission("skgame.history.others")) {
                Messages.send(player, "command.history.no-permission-others");
                return;
            }
            Player online = Bukkit.getPlayerExact(args[1]);
            @SuppressWarnings("deprecation")
            OfflinePlayer offline = online != null ? online : Bukkit.getOfflinePlayer(args[1]);
            subject = offline;
            if (!subject.hasPlayedBefore() && !(subject instanceof Player)) {
                Messages.send(player, "command.history.not-found", args[1]);
                return;
            }
        }
        GameHistoryGuiService.getInstance().openFor(player, subject);
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
        if (session.getRole(target) != null) {
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

    private void handleUnban(Player player, String[] args) {
        if (args.length < 2) return;
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null || !player.equals(session.getHost())) {
            Messages.send(player, "gui.session.error.not-host");
            return;
        }
        String targetName = args[1];
        java.util.UUID targetUuid = null;
        for (java.util.Map.Entry<java.util.UUID, String> e : session.getBannedEntries().entrySet()) {
            if (e.getValue().equalsIgnoreCase(targetName)) { targetUuid = e.getKey(); break; }
        }
        if (targetUuid == null) {
            Messages.send(player, "session.unban.error.not-banned", targetName);
            return;
        }
        session.removeBan(targetUuid);
        Messages.send(player, "session.unban.unbanned", targetName);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) return;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { Messages.send(player, "command.profile.not-found", args[1]); return; }
        SessionLifecycleManagerImpl.getInstance().kickMember(player, target);
    }

    private void handleBan(Player player, String[] args) {
        if (args.length < 2) return;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { Messages.send(player, "command.profile.not-found", args[1]); return; }
        SessionLifecycleManagerImpl.getInstance().banMember(player, target);
    }

    private void handleReport(Player reporter, String[] args) {
        if (!reporter.hasPermission("skgame.report")) {
            Messages.send(reporter, "command.error.no-permission");
            return;
        }
        if (args.length < 3) {
            Messages.send(reporter, "report.usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            Messages.send(reporter, "report.player-not-found");
            return;
        }
        if (target.equals(reporter)) {
            Messages.send(reporter, "report.self");
            return;
        }
        long cooldownMs = cz.nox.skgame.SkGame.getInstance().getConfig()
                .getLong("report.cooldown-seconds", 60L) * 1000L;
        Long last = reportCooldowns.get(reporter.getUniqueId());
        if (last != null) {
            long remaining = (last + cooldownMs - System.currentTimeMillis()) / 1000L;
            if (remaining > 0) {
                Messages.send(reporter, "report.cooldown", remaining);
                return;
            }
        }
        // Collect reason from remaining args
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) reasonBuilder.append(' ');
            reasonBuilder.append(args[i]);
        }
        String reason = reasonBuilder.toString();
        // Determine target's session (or "-" if not in one)
        Session targetSession = SessionManager.getInstance().getSession(target);
        String sessionId = targetSession != null ? targetSession.getId() : "-";

        reportCooldowns.put(reporter.getUniqueId(), System.currentTimeMillis());
        Messages.send(reporter, "report.submitted", target.getName());

        // Notify all online ops/holders of skgame.report.notify
        String notifyKey = "report.notify";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("skgame.report.notify")) {
                Messages.send(p, notifyKey, reporter.getName(), target.getName(), sessionId, reason);
            }
        }
        cz.nox.skgame.SkGame.getInstance().getLogger().info(
                "[Report] " + reporter.getName() + " → " + target.getName()
                + " (" + sessionId + ") | " + reason);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            List<String> opts = new ArrayList<>(List.of("join", "quickplay", "rejoin", "history"));
            if (player.hasPermission("skgame.profile")) opts.add("profile");
            if (player.hasPermission("skgame.spectate")) opts.add("spectate");
            if (player.hasPermission("skgame.admin")) opts.add("admin");
            if (player.hasPermission("skgame.report")) opts.add("report");
            Session ps = SessionManager.getInstance().getSession(player);
            if (ps != null && player.equals(ps.getHost())) {
                opts.add("invite");
                opts.add("uninvite");
                opts.add("kick");
                opts.add("ban");
                if (!ps.getBannedEntries().isEmpty()) opts.add("unban");
            }
            String partial = args[0].toLowerCase();
            return opts.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "join"      -> joinSub.tabComplete(player, args);
                case "quickplay" -> {
                    List<String> qopts = new java.util.ArrayList<>(List.of("cancel"));
                    for (cz.nox.skgame.api.game.model.MinigameTag t :
                            cz.nox.skgame.api.game.model.MinigameTag.values()) {
                        qopts.add(t.name().toLowerCase());
                    }
                    yield qopts.stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "spectate"  -> spectateSub.tabComplete(player, args);
                case "history"   -> player.hasPermission("skgame.history.others")
                        ? onlineNames(args[1]) : List.of();
                case "profile"   -> player.hasPermission("skgame.profile.others")
                        ? onlineNames(args[1]) : List.of();
                case "report"    -> player.hasPermission("skgame.report")
                        ? onlineNames(args[1]) : List.of();
                case "kick", "ban" -> {
                    Session ks = SessionManager.getInstance().getSession(player);
                    if (ks == null || !player.equals(ks.getHost())) yield List.of();
                    String partial = args[1].toLowerCase(java.util.Locale.ROOT);
                    yield ks.getMembers().stream()
                            .filter(m -> !m.equals(player))
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase(java.util.Locale.ROOT).startsWith(partial))
                            .collect(Collectors.toList());
                }
                case "unban" -> {
                    Session us = SessionManager.getInstance().getSession(player);
                    if (us == null || !player.equals(us.getHost())) yield List.of();
                    String partial = args[1].toLowerCase(java.util.Locale.ROOT);
                    yield us.getBannedEntries().values().stream()
                            .filter(n -> n.toLowerCase(java.util.Locale.ROOT).startsWith(partial))
                            .collect(Collectors.toList());
                }
                default          -> List.of();
            };
        }
        return List.of();
    }

    private static List<String> onlineNames(String partial) {
        String low = partial.toLowerCase(java.util.Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase(java.util.Locale.ROOT).startsWith(low))
                .collect(Collectors.toList());
    }
}
