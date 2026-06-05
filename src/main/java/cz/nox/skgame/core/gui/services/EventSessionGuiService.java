package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.EventSessionOpenEvent;
import cz.nox.skgame.api.game.event.SessionSettingsChangedEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
import cz.nox.skgame.api.game.model.type.DisbandReason;
import cz.nox.skgame.api.game.model.type.GameStartReason;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EventSessionGuiService implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static EventSessionGuiService instance;
    private final Set<UUID> activeViewers = new HashSet<>();

    private EventSessionGuiService() {}

    public static synchronized EventSessionGuiService getInstance() {
        if (instance == null) instance = new EventSessionGuiService();
        return instance;
    }

    private Component c(String s) { return LEGACY.deserialize(s); }

    public void openFor(Player admin) {
        Session session = SessionManager.getInstance().getEventSession();
        if (session == null) {
            session = SessionLifecycleManagerImpl.getInstance().createEventSession(admin);
            if (session == null) {
                Messages.send(admin, "event.not-running");
                return;
            }
        }
        admin.openInventory(buildFor(admin, session));
        activeViewers.add(admin.getUniqueId());
    }

    private Inventory buildFor(Player admin, Session session) {
        MiniGame mg = session.getMiniGame();
        GameMap map = session.getGameMap();
        String mgName = mg != null && mg.getValue("name") != null ? mg.getValue("name").toString() : "Not set";
        String mapName = map != null ? map.getId() : "Not set";
        int total = session.getLobbyMembers().size() + session.getPlayers().size();
        boolean unlocked = session.getVisibility() == SessionVisibility.PUBLIC;
        boolean started = session.getState() == SessionState.STARTED;

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(Messages.get("gui.event.slot.gui-title", admin));

        // Black border top + bottom
        GuiItem glass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(" ");
        builder.fill(0, 8, glass).fill(45, 53, glass);

        // Minigame selector
        builder.slot(1, GuiItem.of(mg != null ? Material.BOOK : Material.WRITABLE_BOOK)
                .name(c("&eMiniGame: &f" + mgName))
                .lore(c("&7Click to select"))
                .onClick(e -> MinigamesGuiService.getInstance().openFor((Player) e.getWhoClicked())));

        // Map selector
        builder.slot(3, GuiItem.of(map != null ? Material.MAP : Material.FILLED_MAP)
                .name(c("&eMap: &f" + mapName))
                .lore(c("&7Click to select"))
                .onClick(e -> MapsGuiService.getInstance().openFor((Player) e.getWhoClicked())));

        // Player count
        builder.slot(5, GuiItem.of(Material.PLAYER_HEAD)
                .name(c("&ePlayers in lobby: &f" + total)));

        // State
        builder.slot(7, GuiItem.of(Material.COMPASS)
                .name(c("&eState: &f" + session.getState().name()))
                .lore(c("&eVisibility: &f" + session.getVisibility().name())));

        // Slot 45 — Unlock / Start / Stop
        if (!unlocked) {
            builder.slot(45, GuiItem.of(Material.LIME_STAINED_GLASS_PANE)
                    .name(c("&a&lUnlock Event"))
                    .lore(c("&7Opens the event to all players"))
                    .onClick(e -> unlockEvent((Player) e.getWhoClicked(), session)));
        } else if (!started) {
            builder.slot(45, GuiItem.of(Material.LIME_CONCRETE)
                    .name(c("&a&lStart Game"))
                    .lore(c("&7Start the minigame now"))
                    .onClick(e -> {
                        SessionLifecycleManagerImpl.getInstance().startGame(session, GameStartReason.ADMIN_FORCE, null);
                        openFor((Player) e.getWhoClicked());
                    }));
        } else {
            builder.slot(45, GuiItem.of(Material.RED_CONCRETE)
                    .name(c("&c&lStop Game"))
                    .lore(c("&7Force-stop the running game"))
                    .onClick(e -> {
                        SessionLifecycleManagerImpl.getInstance().endGame(session, "admin");
                        openFor((Player) e.getWhoClicked());
                    }));
        }

        // Slot 47 — Disband
        builder.slot(47, GuiItem.of(Material.BARRIER)
                .name(c("&c&lDisband Event Session"))
                .lore(c("&7Removes the event session entirely"))
                .onClick(e -> {
                    SessionLifecycleManagerImpl.getInstance().disbandSession(session, DisbandReason.EXPLICIT_DISBAND);
                    ((Player) e.getWhoClicked()).closeInventory();
                }));

        // Slot 49 — Refresh
        builder.slot(49, GuiItem.of(Material.CLOCK)
                .name(c("&7Refresh"))
                .onClick(e -> openFor((Player) e.getWhoClicked())));

        // Slot 53 — Back
        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name(c("&c&lBack"))
                .onClick(e -> MainGuiService.getInstance().openFor((Player) e.getWhoClicked())));

        return builder.build();
    }

    private void unlockEvent(Player admin, Session session) {
        session.setVisibility(SessionVisibility.PUBLIC);
        Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(session, "visibility"));
        Bukkit.getPluginManager().callEvent(new EventSessionOpenEvent(session));

        MiniGame mg = session.getMiniGame();
        String mgName = mg != null && mg.getValue("name") != null ? mg.getValue("name").toString() : "Event";
        GameMap map = session.getGameMap();
        String mapName = map != null ? map.getId() : "—";

        for (Player online : Bukkit.getOnlinePlayers()) {
            Messages.send(online, "event.broadcast", mgName, mapName);
            online.playSound(online.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }
        openFor(admin);
    }

    public void update() {
        for (UUID uuid : new HashSet<>(activeViewers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) { activeViewers.remove(uuid); continue; }
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) {
                Session session = SessionManager.getInstance().getEventSession();
                if (session != null) openFor(p); else p.closeInventory();
            } else {
                activeViewers.remove(uuid);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            activeViewers.remove(event.getPlayer().getUniqueId());
        }
    }
}
