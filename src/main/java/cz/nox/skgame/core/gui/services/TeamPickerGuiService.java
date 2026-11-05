package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.model.KitOption;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.TeamEntry;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.team.TeamPickerItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamPickerGuiService implements Listener {

    private static final Material[] TEAM_WOOLS = {
        Material.WHITE_WOOL, Material.RED_WOOL, Material.BLUE_WOOL,
        Material.YELLOW_WOOL, Material.LIME_WOOL, Material.ORANGE_WOOL,
        Material.PURPLE_WOOL, Material.PINK_WOOL
    };

    private static TeamPickerGuiService instance;

    private TeamPickerGuiService() {}

    public static synchronized TeamPickerGuiService getInstance() {
        if (instance == null) instance = new TeamPickerGuiService();
        return instance;
    }

    // ─── GUI ──────────────────────────────────────────────────────────────────

    public void openFor(Player player, Session session) {
        MiniGame mg = session.getMiniGame();
        if (mg == null) return;
        List<String> teams = mg.getTeams();
        if (teams.isEmpty()) return;

        // Count current assignments
        Map<String, Integer> counts = new HashMap<>();
        teams.forEach(t -> counts.put(t, 0));
        for (Player m : session.getLobbyMembers()) {
            String t = session.getTeam(m);
            if (t != null && counts.containsKey(t)) counts.merge(t, 1, Integer::sum);
        }

        String currentTeam = session.getTeam(player);
        // 1 row fits up to 8 team buttons + 1 random at slot 8; overflow to 2 rows
        int teamRows = teams.size() >= 9 ? 2 : 1;
        int randomSlot = teamRows == 1 ? 8 : 17;

        // When the minigame declares kits, append one extra row of kit buttons below the teams.
        List<KitOption> kits = mg.getKits();
        boolean hasKits = !kits.isEmpty();
        int totalRows = teamRows + (hasKits ? 1 : 0);

        GuiBuilder builder = new GuiBuilder()
                .size(totalRows)
                .title(legacy("&6Choose your team"));

        for (int i = 0; i < teams.size() && i < randomSlot; i++) {
            String teamName = teams.get(i);
            boolean mine = teamName.equals(currentTeam);
            int count = counts.getOrDefault(teamName, 0);
            TeamEntry entry = mg.getTeamEntry(teamName);
            ItemStack icon = (entry != null && entry.getIcon() != null)
                    ? entry.getIcon()
                    : new ItemStack(TEAM_WOOLS[i % TEAM_WOOLS.length]);
            String displayName = entry != null ? entry.getDisplayName() : teamName;
            builder.slot(i, GuiItem.of(icon)
                    .name((mine ? "&a✔ " : "&f") + displayName)
                    .lore(legacy("&7" + count + " player" + (count != 1 ? "s" : "")))
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        Session s = SessionManager.getInstance().getSession(p);
                        if (s == null || s.getState() != SessionState.PREPARATION) {
                            p.closeInventory(); return;
                        }
                        s.setTeam(p, teamName);
                        openFor(p, s);
                    }));
        }

        builder.slot(randomSlot, GuiItem.of(Material.COMPASS)
                .name("&7Random")
                .lore(legacy("&8Assign me to the smallest team"))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session s = SessionManager.getInstance().getSession(p);
                    if (s == null || s.getState() != SessionState.PREPARATION) {
                        p.closeInventory(); return;
                    }
                    List<String> t = s.getMiniGame().getTeams();
                    Map<String, Integer> c = new HashMap<>();
                    t.forEach(name -> c.put(name, 0));
                    for (Player m : s.getLobbyMembers()) {
                        String team = s.getTeam(m);
                        if (team != null && c.containsKey(team)) c.merge(team, 1, Integer::sum);
                    }
                    String smallest = t.stream().min(Comparator.comparingInt(c::get)).orElseThrow();
                    s.setTeam(p, smallest);
                    openFor(p, s);
                }));

        if (hasKits) {
            int base = teamRows * 9;
            String currentKit = currentKitId(session, player, kits);
            for (int i = 0; i < kits.size() && i < 9; i++) {
                KitOption kit = kits.get(i);
                boolean selected = kit.id() != null && kit.id().equals(currentKit);
                ItemStack icon = kit.icon() != null ? kit.icon() : new ItemStack(Material.CHEST);
                builder.slot(base + i, GuiItem.of(icon)
                        .name((selected ? "&a✔ " : "&f") + kit.resolvedName())
                        .lore(legacy(selected ? "&7Selected kit" : "&8Click to select this kit"))
                        .onClick(e -> {
                            Player p = (Player) e.getWhoClicked();
                            Session s = SessionManager.getInstance().getSession(p);
                            if (s == null || s.getState() != SessionState.PREPARATION) {
                                p.closeInventory(); return;
                            }
                            // Per-player chosen kit → TEMPORARY session value "kit.<uuid>" = kit id.
                            s.setValue("kit." + p.getUniqueId(), kit.id(), true);
                            openFor(p, s);
                        }));
            }
        }

        player.openInventory(builder.build());
    }

    /** The player's chosen kit id (temporary session value), defaulting to the first declared kit. */
    private static String currentKitId(Session session, Player player, List<KitOption> kits) {
        Object kv = session.getValue("kit." + player.getUniqueId(), true);
        if (kv instanceof String s) return s;
        return kits.isEmpty() ? null : kits.get(0).id();
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!TeamPickerItem.getInstance().isPicker(e.getItem())) return;
        e.setCancelled(true);
        Player player = e.getPlayer();
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null || session.getState() != SessionState.PREPARATION) return;
        openFor(player, session);
    }

    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent e) {
        if (!TeamPickerItem.getInstance().isPicker(e.getItemDrop().getItemStack())) return;
        e.setCancelled(true);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
