package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.team.MiniGameVoteItem;
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

public class MiniGameVoteGuiService implements Listener {

    private static MiniGameVoteGuiService instance;

    private MiniGameVoteGuiService() {}

    public static synchronized MiniGameVoteGuiService getInstance() {
        if (instance == null) instance = new MiniGameVoteGuiService();
        return instance;
    }

    // ─── GUI ──────────────────────────────────────────────────────────────────

    public void openFor(Player player, Session session) {
        List<MiniGame> candidates = getCandidates();
        if (candidates.isEmpty()) return;

        Map<String, Integer> voteCounts = new HashMap<>();
        candidates.forEach(mg -> voteCounts.put(mg.getId(), 0));
        session.getMiniGameVotes().values()
                .forEach(id -> voteCounts.computeIfPresent(id, (k, v) -> v + 1));

        String myVote = session.getMiniGameVote(player);
        int rows = candidates.size() > 9 ? 2 : 1;

        GuiBuilder builder = new GuiBuilder()
                .size(rows)
                .title(legacy("&dVote for a minigame"));

        for (int i = 0; i < candidates.size() && i < rows * 9; i++) {
            MiniGame mg = candidates.get(i);
            boolean mine = mg.getId().equals(myVote);
            int votes = voteCounts.getOrDefault(mg.getId(), 0);
            Object nameObj = mg.getValue("name");
            String displayName = nameObj != null ? nameObj.toString() : mg.getId();

            builder.slot(i, GuiItem.of(Material.PAPER)
                    .name((mine ? "&a✔ " : "&f") + displayName)
                    .lore(legacy("&7Votes: &f" + votes))
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        Session s = SessionManager.getInstance().getSession(p);
                        if (s == null || s.getState() != SessionState.PREPARATION) {
                            p.closeInventory(); return;
                        }
                        s.setMiniGameVote(p, mg.getId());
                        openFor(p, s);
                    }));
        }

        player.openInventory(builder.build());
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!MiniGameVoteItem.getInstance().isVoteItem(e.getItem())) return;
        e.setCancelled(true);
        Player player = e.getPlayer();
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null || session.getState() != SessionState.PREPARATION) return;
        if (!session.isMiniGameVoting()) return;
        openFor(player, session);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e) {
        if (!MiniGameVoteItem.getInstance().isVoteItem(e.getItemDrop().getItemStack())) return;
        e.setCancelled(true);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private List<MiniGame> getCandidates() {
        MiniGameManager mgm = MiniGameManager.getInstance();
        return Arrays.stream(mgm.getAllMiniGames())
                .filter(mg -> !mgm.isMinigameDisabled(mg.getId()))
                .sorted(Comparator.comparing(MiniGame::getId))
                .collect(Collectors.toList());
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
