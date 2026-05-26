package cz.nox.skgame.core.command.subcommands;

import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.gui.services.MainGuiService;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class FilterSubcommand {

    public void execute(Player player, String[] args) {
        MainGuiService gui = MainGuiService.getInstance();

        if (args.length < 2 || args[1].equalsIgnoreCase("clear")) {
            gui.setFilter(player, null);
            Messages.send(player, "gui.filter.cleared");
            // Reopen main GUI if still viewing it
            reopenIfActive(player, gui);
            return;
        }

        // Join remaining args as the filter text
        String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        gui.setFilter(player, text);
        Messages.send(player, "gui.filter.set", text);
        reopenIfActive(player, gui);
    }

    private static void reopenIfActive(Player player, MainGuiService gui) {
        if (player.getOpenInventory().getTopInventory().getHolder()
                instanceof cz.nox.skgame.api.gui.GuiHolder) {
            gui.openFor(player);
        }
    }
}
