package cz.nox.skgame.core.command.subcommands;

import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JoinSubcommand {

    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            Messages.send(player, "command.join.usage");
            return;
        }
        if (SessionManager.getInstance().getSession(player) != null) {
            Messages.send(player, "command.join.already-in-session");
            return;
        }
        Session session = SessionManager.getInstance().getSessionById(args[1]);
        if (session == null) {
            Messages.send(player, "command.join.not-found", args[1]);
            return;
        }
        SessionLifecycleManagerImpl.getInstance().joinSession(player, session);
    }

    public List<String> tabComplete(Player player, String[] args) {
        String partial = args.length >= 2 ? args[1].toLowerCase() : "";
        // Only expose public session IDs — private sessions are not discoverable
        return Arrays.stream(SessionManager.getInstance().getAllSessions())
                .filter(s -> !"private".equals(s.getValue("mode", false)))
                .map(Session::getId)
                .filter(id -> id.startsWith(partial))
                .collect(Collectors.toList());
    }
}
