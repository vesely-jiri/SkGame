package cz.nox.skgame.core.command;

import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.command.subcommands.AdminSubcommand;
import cz.nox.skgame.core.command.subcommands.SpectateSubcommand;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.gui.services.MainGuiService;
import cz.nox.skgame.core.gui.services.SessionGuiService;
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
            case "spectate" -> spectateSub.execute(player, args);
            default         -> Messages.send(player, "command.error.unknown-subcommand");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            if (player.hasPermission("skgame.spectate")) opts.add("spectate");
            if (player.hasPermission("skgame.admin")) opts.add("admin");
            String partial = args[0].toLowerCase();
            return opts.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spectate")) {
            return spectateSub.tabComplete(player, args);
        }
        return List.of();
    }
}
