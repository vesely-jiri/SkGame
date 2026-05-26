package cz.nox.skgame.core.command.subcommands;

import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
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
        String token = args[1];
        SessionManager sm = SessionManager.getInstance();
        // Try by ID first, then by join code
        Session session = sm.getSessionById(token);
        if (session == null) session = sm.getSessionByCode(token);
        if (session == null) {
            Messages.send(player, "command.join.not-found", token);
            return;
        }
        // Access control for non-PUBLIC sessions
        SessionVisibility vis = session.getVisibility();
        if (vis == SessionVisibility.INVITE_ONLY) {
            if (!session.isInvited(player.getUniqueId())) {
                Messages.send(player, "session.invite.not-invited");
                return;
            }
        } else if (vis == SessionVisibility.CODE) {
            // Must have joined via code (session.getJoinCode() matched)
            if (sm.getSessionByCode(token) == null) {
                Messages.send(player, "session.code.invalid");
                return;
            }
        }
        SessionLifecycleManagerImpl.getInstance().joinSession(player, session);
    }

    public List<String> tabComplete(Player player, String[] args) {
        String partial = args.length >= 2 ? args[1].toLowerCase() : "";
        // Only expose public session IDs — private sessions are not discoverable
        return Arrays.stream(SessionManager.getInstance().getAllSessions())
                .filter(s -> s.getVisibility() == SessionVisibility.PUBLIC)
                .map(Session::getId)
                .filter(id -> id.startsWith(partial))
                .collect(Collectors.toList());
    }
}
