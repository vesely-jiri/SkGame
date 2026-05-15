package cz.nox.skgame.core.command.subcommands;

import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.gui.services.AdminGuiService;
import org.bukkit.entity.Player;

public class AdminSubcommand {

    public void execute(Player player, String[] args) {
        if (!player.hasPermission("skgame.admin")) {
            Messages.send(player, "command.error.no-permission");
            return;
        }
        AdminGuiService.getInstance().openAdminGui(player);
    }
}
