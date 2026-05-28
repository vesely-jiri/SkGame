package cz.nox.skgame.core.listener;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.core.game.SessionManager;
import io.papermc.paper.event.player.AsyncChatEvent;
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
@SuppressWarnings("deprecation")
public class ChatIsolationListener implements Listener {

    /**
     * Legacy path — cancel before CMI (NORMAL) delivers, then re-send to the isolated set.
     * ignoreCancelled = true: if a mute/anti-spam plugin cancelled at LOWEST we must not
     * resurrect the message via manual delivery.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        SkGame plugin = SkGame.getInstance();
        if (!plugin.getConfig().getBoolean("session.chat.isolation", false)) return;

        Player sender = event.getPlayer();
        Session session = SessionManager.getInstance().getSession(sender);
        if (session == null) return;

        event.setCancelled(true);

        boolean spectatorIsolation = plugin.getConfig().getBoolean("session.chat.spectator-isolation", false);
        Set<Player> allowed = computeAllowed(sender, session, spectatorIsolation, event.getRecipients());
        String formatted = formatMessage(event);
        for (Player recipient : allowed) {
            recipient.sendMessage(formatted);
        }
    }

    /**
     * Paper path — clear viewers for isolated senders to suppress native delivery.
     * Does not cancel to avoid interfering with the legacy compat bridge fire order.
     * Delivery already happened once in onLegacyChat.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event) {
        SkGame plugin = SkGame.getInstance();
        if (!plugin.getConfig().getBoolean("session.chat.isolation", false)) return;

        Player sender = event.getPlayer();
        Session session = SessionManager.getInstance().getSession(sender);
        if (session == null) return;

        event.viewers().clear();
    }

    private Set<Player> computeAllowed(Player sender, Session session, boolean spectatorIsolation,
                                       Iterable<Player> candidates) {
        SessionRole senderRole = session.getRole(sender);
        Set<Player> allowed = new HashSet<>();
        for (Player candidate : candidates) {
            Session candidateSession = SessionManager.getInstance().getSession(candidate);
            if (candidateSession == null || !candidateSession.getId().equals(session.getId())) continue;
            if (spectatorIsolation && senderRole != null) {
                SessionRole candidateRole = session.getRole(candidate);
                boolean senderIsSpec = senderRole == SessionRole.SPECTATOR;
                boolean candidateIsSpec = candidateRole == SessionRole.SPECTATOR;
                if (senderIsSpec != candidateIsSpec) continue;
            }
            allowed.add(candidate);
        }
        return allowed;
    }

    private String formatMessage(AsyncPlayerChatEvent event) {
        try {
            return String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
        } catch (Exception e) {
            return "<" + event.getPlayer().getDisplayName() + "> " + event.getMessage();
        }
    }
}
