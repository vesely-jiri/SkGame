package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.PlayerRoleChangeEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.TeamEntry;
import cz.nox.skgame.api.game.model.TeamRules;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.api.game.model.type.SessionState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles per-session team rules: friendly-fire (damage cancel), collision, and nametag visibility
 * via a dedicated per-session scoreboard (does not touch the main scoreboard; no TabManager conflict).
 */
public class TeamRulesManager implements Listener {

    private static TeamRulesManager instance;

    /** sessionId → scoreboard used for collision/nametag rules while game is running */
    private final Map<String, Scoreboard> sessionBoards = new ConcurrentHashMap<>();

    private TeamRulesManager() {}

    public static synchronized TeamRulesManager getInstance() {
        if (instance == null) instance = new TeamRulesManager();
        return instance;
    }

    // ─── Bukkit event hooks ───────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameStart(GameStartEvent e) {
        applyRules(e.getSession());
    }

    /** Player returns to LOBBY role = game ended for them → restore scoreboard. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRoleChange(PlayerRoleChangeEvent e) {
        if (e.getTo() == SessionRole.LOBBY
                && (e.getFrom() == SessionRole.PLAYER || e.getFrom() == SessionRole.SPECTATOR)) {
            restoreScoreboard(e.getPlayer());
        }
    }

    /** Player fully removed from session — safety restore. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeaveSession(GamePlayerSessionLeave e) {
        restoreScoreboard(e.getPlayer());
    }

    /** Session disbanded — clean up board entry. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSessionDisband(SessionDisbandEvent e) {
        sessionBoards.remove(e.getSession().getId());
    }

    /** Friendly-fire check: cancel damage between teammates when friendlyFire = false. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        Session session = SessionManager.getInstance().getSession(victim);
        if (session == null || session.getState() != SessionState.STARTED) return;
        if (!session.getMembers().contains(attacker)) return;

        String victimTeam = session.getTeam(victim);
        String attackerTeam = session.getTeam(attacker);
        if (victimTeam == null || !victimTeam.equals(attackerTeam)) return;

        MiniGame mg = session.getMiniGame();
        if (mg == null) return;
        TeamRules rules = mg.getEffectiveRules(victimTeam);
        if (!rules.friendlyFire()) e.setCancelled(true);
    }

    // ─── Scoreboard management ────────────────────────────────────────────────

    private void applyRules(Session session) {
        MiniGame mg = session.getMiniGame();
        if (mg == null || mg.getTeamEntries().isEmpty()) return;

        // Skip if no custom rules declared
        boolean hasCustomRules = mg.getDefaultTeamRules() != null
                || mg.getTeamEntries().stream().anyMatch(te -> te.getRules() != null);
        if (!hasCustomRules) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        for (TeamEntry te : mg.getTeamEntries()) {
            TeamRules rules = mg.getEffectiveRules(te.getId());
            Team vanillaTeam = board.registerNewTeam(te.getId());
            vanillaTeam.setAllowFriendlyFire(rules.friendlyFire());
            vanillaTeam.setOption(Team.Option.COLLISION_RULE,
                    rules.collision() ? Team.OptionStatus.ALWAYS : Team.OptionStatus.FOR_OTHER_TEAMS);
            vanillaTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, rules.nametag().toOptionStatus());

            for (Player p : session.getMembers()) {
                if (te.getId().equals(session.getTeam(p))) {
                    vanillaTeam.addEntry(p.getName());
                }
            }
        }

        for (Player p : session.getMembers()) {
            p.setScoreboard(board);
        }
        sessionBoards.put(session.getId(), board);
    }

    private void restoreScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
