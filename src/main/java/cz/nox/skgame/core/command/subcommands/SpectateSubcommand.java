package cz.nox.skgame.core.command.subcommands;

import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpectateSubcommand {

    public void execute(Player player, String[] args) {
        if (!player.hasPermission("skgame.spectate")) {
            Messages.send(player, "command.error.no-permission");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("Usage: /game spectate <session-id>");
            return;
        }
        SessionManager sm = SessionManager.getInstance();
        Session session = sm.getSessionById(args[1]);
        if (session == null) {
            Messages.send(player, "spectator.session-not-found", args[1]);
            return;
        }
        if (sm.getSession(player) != null) {
            Messages.send(player, "spectator.already-in-session");
            return;
        }
        boolean joined = SessionLifecycleManagerImpl.getInstance().joinAsSpectator(player, session);
        if (!joined) {
            Messages.send(player, "spectator.join-denied");
        }
    }

    public List<String> tabComplete(Player player, String[] args) {
        String partial = args.length >= 2 ? args[1].toLowerCase() : "";
        return Arrays.stream(SessionManager.getInstance().getAllSessions())
                .filter(s -> s.getState() == SessionState.LOBBY || s.getState() == SessionState.STARTED)
                .map(Session::getId)
                .filter(id -> id.startsWith(partial))
                .collect(Collectors.toList());
    }
}
