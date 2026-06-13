package cz.nox.skgame.core.command.subcommands;

import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import cz.nox.skgame.core.gui.services.SessionGuiService;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
        // Token-based invite: /game join <sessionId> <uuidToken>
        if (args.length >= 3) {
            try {
                UUID inviteToken = UUID.fromString(args[2]);
                if (!session.consumeInviteToken(inviteToken, player.getUniqueId())) {
                    Messages.send(player, "session.invite.expired");
                    return;
                }
                // Valid token consumed — bypass visibility check and join directly
                session.removeInvitedPlayer(player.getUniqueId());
                boolean joined = SessionLifecycleManagerImpl.getInstance().joinSession(player, session);
                if (joined) {
                    Messages.send(player, "session.joined");
                    SessionGuiService.getInstance().openFor(player);
                }
                return;
            } catch (IllegalArgumentException ignored) {
                // args[2] is not a UUID — fall through to normal visibility check
            }
        }

        // Normal join: access control for non-PUBLIC sessions
        SessionVisibility vis = session.getVisibility();
        if (vis == SessionVisibility.INVITE_ONLY) {
            if (!session.isInvited(player.getUniqueId())) {
                Messages.send(player, "session.invite.not-invited");
                return;
            }
        } else if (vis == SessionVisibility.CODE) {
            // Invited players can join via session ID without providing the code
            boolean hasCode = sm.getSessionByCode(token) != null;
            boolean invited = session.isInvited(player.getUniqueId());
            if (!hasCode && !invited) {
                Messages.send(player, "session.code.invalid");
                return;
            }
        }
        boolean joined = SessionLifecycleManagerImpl.getInstance().joinSession(player, session);
        if (joined) {
            // Consume the invite so it cannot be reused after leaving
            session.removeInvitedPlayer(player.getUniqueId());
            Messages.send(player, "session.joined");
            SessionGuiService.getInstance().openFor(player);
        }
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
