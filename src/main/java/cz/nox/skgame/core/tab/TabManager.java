package cz.nox.skgame.core.tab;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.GamePlayerSessionJoin;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.PlayerRoleChangeEvent;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.event.SpectatorJoinEvent;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.SessionManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TAB list separation and per-session color-coding.
 *
 * Two-tier design:
 *   1. Lobby coloring  — Teams on the main scoreboard ("skg-{8hex}" names, session palette color).
 *      Only LOBBY-role session members are entries; arena players are never entries.
 *   2. Arena isolation — hidePlayer/showPlayer for PLAYER/SPECTATOR role members of running sessions.
 *      Bidirectional: arena players hidden from lobby and other sessions.
 *
 * Enabled only when config key `tab.separation.enabled` is true (default false).
 */
public class TabManager implements Listener {

    private static final List<NamedTextColor> PALETTE = List.of(
            NamedTextColor.YELLOW,
            NamedTextColor.GREEN,
            NamedTextColor.AQUA,
            NamedTextColor.LIGHT_PURPLE,
            NamedTextColor.GOLD,
            NamedTextColor.RED,
            NamedTextColor.DARK_GREEN,
            NamedTextColor.DARK_AQUA,
            NamedTextColor.DARK_RED,
            NamedTextColor.BLUE,
            NamedTextColor.DARK_PURPLE,
            NamedTextColor.WHITE
    );

    private static TabManager instance;

    /** sessionId → index into PALETTE assigned at session creation. */
    private final Map<String, Integer> sessionColorIdx = new ConcurrentHashMap<>();
    /** UUIDs of players currently in PLAYER or SPECTATOR role in a STARTED/ENDED session. */
    private final Set<UUID> arenaPlayers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger colorCounter = new AtomicInteger(0);

    private TabManager() {}

    public static synchronized TabManager getInstance() {
        if (instance == null) instance = new TabManager();
        return instance;
    }

    // ─── Config ──────────────────────────────────────────────────────────────

    private boolean enabled() {
        return SkGame.getInstance().getConfig().getBoolean("tab.separation.enabled", false);
    }

    // ─── Team helpers ─────────────────────────────────────────────────────────

    private static String teamName(String sessionId) {
        return "skg-" + String.format("%08x", sessionId.hashCode());
    }

    private static Scoreboard mainBoard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    private Team ensureTeam(String sessionId) {
        String name = teamName(sessionId);
        Scoreboard board = mainBoard();
        Team team = board.getTeam(name);
        if (team == null) team = board.registerNewTeam(name);
        // Assign color lazily on first call for this session (computeIfAbsent = once, stable).
        // Always re-apply — handles the case where the team pre-existed without a color.
        int idx = sessionColorIdx.computeIfAbsent(sessionId,
                k -> colorCounter.getAndIncrement() % PALETTE.size());
        team.color(PALETTE.get(idx));
        return team;
    }

    private void addToColorTeam(String sessionId, Player player) {
        ensureTeam(sessionId).addEntry(player.getName());
    }

    private void removeFromColorTeam(String sessionId, Player player) {
        Team team = mainBoard().getTeam(teamName(sessionId));
        if (team != null) team.removeEntry(player.getName());
    }

    private void unregisterTeam(String sessionId) {
        Team team = mainBoard().getTeam(teamName(sessionId));
        if (team != null) { try { team.unregister(); } catch (IllegalStateException ignored) {} }
        sessionColorIdx.remove(sessionId);
    }

    // ─── Arena isolation ──────────────────────────────────────────────────────

    /** Apply bidirectional hide between player and all online players NOT in their session. */
    private void applyArenaHide(Player player, Session session) {
        arenaPlayers.add(player.getUniqueId());
        Plugin plugin = SkGame.getInstance();
        Set<Player> sessionMembers = session.getMembers();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player) || sessionMembers.contains(online)) continue;
            try { online.hidePlayer(plugin, player); } catch (Exception ignored) {}
            try { player.hidePlayer(plugin, online); } catch (Exception ignored) {}
        }
    }

    /** Reverse all hides for a player returning to lobby. Idempotent. */
    private void restoreFromArena(Player player) {
        if (!arenaPlayers.remove(player.getUniqueId())) return;
        Plugin plugin = SkGame.getInstance();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            try { online.showPlayer(plugin, player); } catch (Exception ignored) {}
            try { player.showPlayer(plugin, online); } catch (Exception ignored) {}
        }
    }

    // ─── Event handlers ───────────────────────────────────────────────────────

    /** Session registered → pre-create team with stable color (best-effort; ensureTeam is lazy fallback). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSessionCreate(SessionCreateEvent e) {
        if (!enabled()) return;
        ensureTeam(e.getSession().getId());
    }

    /**
     * Player added to session (any role). If LOBBY-role → add to color team.
     * Covers: initial host join, joinSession, lobby re-entry.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoinSession(GamePlayerSessionJoin e) {
        if (!enabled()) return;
        Session session = e.getSession();
        Player player = e.getPlayer();
        if (session.getRole(player) == SessionRole.LOBBY) {
            addToColorTeam(session.getId(), player);
        }
    }

    /**
     * Role changed within a session.
     *   old == LOBBY → remove from color team (moving to arena or spectator)
     *   new == LOBBY → restore from arena (if applicable) + add to color team (game end / makePlayer)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRoleChange(PlayerRoleChangeEvent e) {
        if (!enabled()) return;
        Player player = e.getPlayer();
        Session session = e.getSession();
        SessionRole from = e.getFrom();
        SessionRole to = e.getTo();

        if (from == SessionRole.LOBBY) {
            removeFromColorTeam(session.getId(), player);
        }
        if (to == SessionRole.LOBBY) {
            restoreFromArena(player);
            addToColorTeam(session.getId(), player);
        }
    }

    /**
     * Game started → bulk-hide all session players from non-session online players.
     * Role transitions (LOBBY→PLAYER) already fired via setRole() before this event.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameStart(GameStartEvent e) {
        if (!enabled()) return;
        Session session = e.getSession();
        Set<Player> members = session.getMembers(); // PLAYER + SPECTATOR + LOBBY (if any remain)
        for (Player p : members) {
            SessionRole role = session.getRole(p);
            if (role == SessionRole.PLAYER || role == SessionRole.SPECTATOR) {
                applyArenaHide(p, session);
            }
        }
    }

    /** Spectator joined a running session mid-game → apply arena hide for that player. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpectatorJoin(SpectatorJoinEvent e) {
        if (!enabled()) return;
        applyArenaHide(e.getPlayer(), e.getSession());
    }

    /**
     * Player removed from session entirely.
     * Covers: explicit leave, eviction, session disband cleanup.
     * Remove from color team + restore from arena if applicable.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeaveSession(GamePlayerSessionLeave e) {
        if (!enabled()) return;
        Player player = e.getPlayer();
        String sessionId = e.getSession().getId();
        removeFromColorTeam(sessionId, player);
        restoreFromArena(player); // no-op if not in arenaPlayers
    }

    /** Session disbanded → unregister color team + safety-restore any remaining arena players. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSessionDisband(SessionDisbandEvent e) {
        if (!enabled()) {
            unregisterTeam(e.getSession().getId());
            return;
        }
        Session session = e.getSession();
        for (Player p : session.getMembers()) {
            restoreFromArena(p);
        }
        unregisterTeam(session.getId());
    }

    /** Player quits → remove ghost entries from color team + clean arenaPlayers set. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        arenaPlayers.remove(player.getUniqueId());
        if (!enabled()) return;
        // Remove name entry from any session's color team (name stays on team otherwise)
        Session session = SessionManager.getInstance().getSession(player);
        if (session != null) removeFromColorTeam(session.getId(), player);
    }

    /**
     * Player (re)joins the server.
     * Case A — player is in a running session as PLAYER/SPECTATOR (rejoin scenario):
     *   re-add to arenaPlayers + re-apply bidirectional hide vs. non-session players.
     * Case B — lobby player:
     *   hide all existing arena players from them + hide them from all arena players
     *   that are not in the same session (none, since they're lobby).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!enabled()) return;
        Player joiner = e.getPlayer();
        Plugin plugin = SkGame.getInstance();

        Session joinerSession = SessionManager.getInstance().getSession(joiner);
        boolean joinerIsArena = joinerSession != null
                && (joinerSession.getState() == SessionState.STARTED || joinerSession.getState() == SessionState.ENDED)
                && (joinerSession.getRole(joiner) == SessionRole.PLAYER
                        || joinerSession.getRole(joiner) == SessionRole.SPECTATOR);

        if (joinerIsArena) {
            // Rejoining into a live arena — re-apply hide vs non-session players
            applyArenaHide(joiner, joinerSession);
        } else {
            // Lobby join — cross-hide with all current arena players from other sessions
            Set<Player> joinerSessionMembers = joinerSession != null ? joinerSession.getMembers() : Set.of();
            for (UUID arenaUuid : arenaPlayers) {
                Player arenaP = Bukkit.getPlayer(arenaUuid);
                if (arenaP == null || arenaP.equals(joiner)) continue;
                if (joinerSessionMembers.contains(arenaP)) continue;
                try { joiner.hidePlayer(plugin, arenaP); } catch (Exception ignored) {}
                try { arenaP.hidePlayer(plugin, joiner); } catch (Exception ignored) {}
            }
        }
    }
}
