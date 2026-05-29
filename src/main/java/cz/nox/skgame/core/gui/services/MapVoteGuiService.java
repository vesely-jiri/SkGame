package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.team.MapVoteItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapVoteGuiService implements Listener {

    private static MapVoteGuiService instance;

    private MapVoteGuiService() {}

    public static synchronized MapVoteGuiService getInstance() {
        if (instance == null) instance = new MapVoteGuiService();
        return instance;
    }

    // ─── GUI ──────────────────────────────────────────────────────────────────

    public void openFor(Player player, Session session) {
        MiniGame mg = session.getMiniGame();
        if (mg == null) return;

        List<GameMap> candidates = getCandidates(session, mg);
        if (candidates.isEmpty()) return;

        // Count current votes per candidate
        Map<String, Integer> voteCounts = new HashMap<>();
        candidates.forEach(m -> voteCounts.put(m.getId(), 0));
        session.getMapVotes().values()
                .forEach(id -> voteCounts.computeIfPresent(id, (k, v) -> v + 1));

        String myVote = session.getMapVote(player);
        // 1 row fits up to 9 map buttons; overflow to 2 rows
        int rows = candidates.size() > 9 ? 2 : 1;

        GuiBuilder builder = new GuiBuilder()
                .size(rows)
                .title(legacy("&bVote for a map"));

        for (int i = 0; i < candidates.size() && i < rows * 9; i++) {
            GameMap map = candidates.get(i);
            boolean mine = map.getId().equals(myVote);
            int votes = voteCounts.getOrDefault(map.getId(), 0);
            Object nameObj = map.getValue("name");
            String displayName = nameObj != null ? nameObj.toString() : map.getId();
            boolean taken = !map.hasArenaSlots()
                    && GameMapManager.getInstance().isMapClaimed(map.getId());

            builder.slot(i, GuiItem.of(Material.FILLED_MAP)
                    .name((mine ? "&a✔ " : "&f") + displayName + (taken ? " &7(taken)" : ""))
                    .lore(legacy("&7Votes: &f" + votes))
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        Session s = SessionManager.getInstance().getSession(p);
                        if (s == null || s.getState() != SessionState.PREPARATION) {
                            p.closeInventory(); return;
                        }
                        s.setMapVote(p, map.getId());
                        openFor(p, s);
                    }));
        }

        player.openInventory(builder.build());
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!MapVoteItem.getInstance().isVoteItem(e.getItem())) return;
        e.setCancelled(true);
        Player player = e.getPlayer();
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null || session.getState() != SessionState.PREPARATION) return;
        if (!session.isMapVoting()) return;
        openFor(player, session);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e) {
        if (!MapVoteItem.getInstance().isVoteItem(e.getItemDrop().getItemStack())) return;
        e.setCancelled(true);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<GameMap> getCandidates(Session session, MiniGame mg) {
        GameMapManager gmm = GameMapManager.getInstance();
        return Arrays.stream(gmm.getGameMaps())
                .filter(m -> m.supportsMiniGame(mg))
                .filter(m -> m.hasArenaSlots() || !gmm.isMapClaimed(m.getId()))
                .sorted(Comparator.comparing(GameMap::getId))
                .collect(Collectors.toList());
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
