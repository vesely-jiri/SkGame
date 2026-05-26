package cz.nox.skgame.core.command.subcommands;

import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.quickplay.QuickplayQueue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Set;

public class QuickplaySubcommand {

    public void execute(Player player, String[] args) {
        if (SessionManager.getInstance().getSession(player) != null) {
            Messages.send(player, "session.error.already-in-session");
            return;
        }

        QuickplayQueue queue = QuickplayQueue.getInstance();

        if (args.length >= 2 && args[1].equalsIgnoreCase("cancel")) {
            if (queue.dequeue(player.getUniqueId())) {
                Messages.send(player, "quickplay.cancelled");
            } else {
                Messages.send(player, "quickplay.not-queued");
            }
            return;
        }

        if (queue.isQueued(player.getUniqueId())) {
            Messages.send(player, "quickplay.already-queued");
            return;
        }

        Set<MinigameTag> tags = EnumSet.noneOf(MinigameTag.class);
        for (int i = 1; i < args.length; i++) {
            try {
                tags.add(MinigameTag.valueOf(args[i].toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                Messages.send(player, "quickplay.unknown-tag", args[i]);
                return;
            }
        }

        queue.enqueue(player, tags);
        if (tags.isEmpty()) {
            Messages.send(player, "quickplay.queued");
        } else {
            String tagStr = tags.stream()
                    .map(MinigameTag::displayName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            Messages.send(player, "quickplay.queued-with-tags", tagStr);
        }

        // Clickable cancel button sent once on queue entry
        Component cancelChip = LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&7[")
                .append(Component.text("Click to cancel", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/game quickplay cancel"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Cancel quickplay search", NamedTextColor.GRAY))))
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize("&7]"));
        player.sendMessage(cancelChip);
    }
}
