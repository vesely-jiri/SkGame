package cz.nox.skgame.core.game.quickplay;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuickplayQueue {

    public static final class QuickplayEntry {
        public final UUID playerId;
        public final Set<MinigameTag> tags;
        public final long joinedAt;

        public QuickplayEntry(UUID playerId, Set<MinigameTag> tags) {
            this.playerId = playerId;
            this.tags = Set.copyOf(tags);
            this.joinedAt = System.currentTimeMillis();
        }
    }

    private static QuickplayQueue instance;
    private final Map<UUID, QuickplayEntry> entries = new ConcurrentHashMap<>();
    private BukkitRunnable searchTask;

    private QuickplayQueue() {}

    public static synchronized QuickplayQueue getInstance() {
        if (instance == null) instance = new QuickplayQueue();
        return instance;
    }

    public boolean isSearchActive() { return searchTask != null; }

    public void enqueue(Player player, Set<MinigameTag> tags) {
        entries.put(player.getUniqueId(), new QuickplayEntry(player.getUniqueId(), tags));
        ensureTaskRunning();
    }

    public boolean dequeue(UUID playerId) {
        return entries.remove(playerId) != null;
    }

    public boolean isQueued(UUID playerId) {
        return entries.containsKey(playerId);
    }

    public Collection<QuickplayEntry> getEntries() {
        return entries.values();
    }

    public void shutdown() {
        if (searchTask != null) {
            searchTask.cancel();
            searchTask = null;
        }
        entries.clear();
    }

    private void ensureTaskRunning() {
        if (searchTask != null) return;
        searchTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (entries.isEmpty()) {
                    cancel();
                    searchTask = null;
                    return;
                }
                tick();
            }
        };
        searchTask.runTaskTimer(SkGame.getInstance(), 0L, 20L);
    }

    private void tick() {
        SkGame plugin = SkGame.getInstance();
        int windowSecs = plugin.getConfig().getInt("quickplay.search-window-seconds", 15);
        boolean createOnNoMatch = plugin.getConfig().getBoolean("quickplay.create-on-no-match", true);
        long now = System.currentTimeMillis();

        SessionManager sm = SessionManager.getInstance();
        SessionLifecycleManagerImpl lifecycle = SessionLifecycleManagerImpl.getInstance();

        Iterator<Map.Entry<UUID, QuickplayEntry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, QuickplayEntry> e = it.next();
            Player player = Bukkit.getPlayer(e.getKey());
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }
            if (sm.getSession(player) != null) {
                it.remove();
                continue;
            }

            QuickplayEntry qe = e.getValue();
            int elapsed = (int)((now - qe.joinedAt) / 1000);

            Session match = findMatch(qe.tags, sm, player);
            if (match != null) {
                it.remove();
                if (lifecycle.joinSession(player, match)) {
                    Messages.send(player, "quickplay.found", match.getId());
                }
                continue;
            }

            if (elapsed >= windowSecs) {
                it.remove();
                if (createOnNoMatch) {
                    if (lifecycle.createSession(player) != null) {
                        Messages.send(player, "quickplay.created-session");
                    }
                } else {
                    Messages.send(player, "quickplay.no-match");
                }
            } else {
                int remaining = windowSecs - elapsed;
                String raw = Messages.get("quickplay.searching", player, remaining);
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
            }
        }
    }

    private @org.jetbrains.annotations.Nullable Session findMatch(Set<MinigameTag> requiredTags, SessionManager sm, Player player) {
        for (Session s : sm.getAllSessions()) {
            if (s.getState() != SessionState.LOBBY) continue;
            if (s.getVisibility() != SessionVisibility.PUBLIC) continue;
            if (s.isBanned(player.getUniqueId())) continue; // skip sessions the player is banned from
            if (requiredTags.isEmpty()) return s;
            if (s.getMiniGame() == null) continue;
            for (MinigameTag t : requiredTags) {
                if (s.getMiniGame().getTags().contains(t)) return s;
            }
        }
        return null;
    }
}
