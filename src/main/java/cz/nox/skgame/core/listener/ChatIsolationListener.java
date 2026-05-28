package cz.nox.skgame.core.listener;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.core.game.SessionManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Restricts chat delivery to session members when session.chat.isolation is enabled.
 * Covers both Paper (AsyncChatEvent) and legacy-Bukkit (AsyncPlayerChatEvent) paths so
 * plugins like CMI that cancel + re-deliver on the legacy event are also isolated.
 * Reads session state without writing — safe for async context.
 */
public class ChatIsolationListener implements Listener {

    /**
     * Legacy path — runs at LOW so CMI (typically NORMAL) sees the pre-filtered recipient set
     * when it builds its own custom delivery list.
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        SkGame plugin = SkGame.getInstance();
        if (!plugin.getConfig().getBoolean("session.chat.isolation", false)) return;

        Player sender = event.getPlayer();
        Session session = SessionManager.getInstance().getSession(sender);
        if (session == null) return;

        boolean spectatorIsolation = plugin.getConfig().getBoolean("session.chat.spectator-isolation", false);
        Set<Player> blocked = computeBlocked(sender, session, spectatorIsolation, event.getRecipients());
        event.getRecipients().removeAll(blocked);
    }

    /** Paper path — non-player audiences (console) are never removed. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event) {
        SkGame plugin = SkGame.getInstance();
        if (!plugin.getConfig().getBoolean("session.chat.isolation", false)) return;

        Player sender = event.getPlayer();
        Session session = SessionManager.getInstance().getSession(sender);
        if (session == null) return;

        boolean spectatorIsolation = plugin.getConfig().getBoolean("session.chat.spectator-isolation", false);

        Set<Player> playerViewers = new HashSet<>();
        for (Audience a : event.viewers()) {
            if (a instanceof Player p) playerViewers.add(p);
        }
        Set<Player> blocked = computeBlocked(sender, session, spectatorIsolation, playerViewers);

        Set<Audience> toRemove = new HashSet<>();
        for (Audience a : event.viewers()) {
            if (a instanceof Player p && blocked.contains(p)) toRemove.add(a);
        }
        event.viewers().removeAll(toRemove);
    }

    private Set<Player> computeBlocked(Player sender, Session session, boolean spectatorIsolation,
                                       Iterable<Player> candidates) {
        SessionRole senderRole = session.getRole(sender);
        Set<Player> blocked = new HashSet<>();
        for (Player recipient : candidates) {
            Session recipientSession = SessionManager.getInstance().getSession(recipient);
            if (recipientSession == null || !recipientSession.getId().equals(session.getId())) {
                blocked.add(recipient);
            } else if (spectatorIsolation && senderRole != null) {
                SessionRole recipientRole = session.getRole(recipient);
                boolean senderIsSpec = senderRole == SessionRole.SPECTATOR;
                boolean recipientIsSpec = recipientRole == SessionRole.SPECTATOR;
                if (senderIsSpec != recipientIsSpec) {
                    blocked.add(recipient);
                }
            }
        }
        return blocked;
    }
}
