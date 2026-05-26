package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.gui.event.MainGuiOpenEvent;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import cz.nox.skgame.core.gui.services.LeaderboardGuiService;
import cz.nox.skgame.core.gui.services.PlayerProfileGuiService;
import cz.nox.skgame.core.gui.services.SessionGuiService;
import cz.nox.skgame.core.gui.services.SpectateGuiService;
import cz.nox.skgame.core.storage.DatabaseManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MainGuiService implements Listener {

    private static final int[] GRAY_SLOTS    = {0, 2, 8, 10, 18, 20, 26, 28, 36, 38, 44, 50, 52};
    private static final int[] RED_SLOTS     = {4, 12, 22, 30, 40, 48};
    // Slot 51 excluded — .sk sets black glass then immediately overwrites with BOOK; net = BOOK.
    // Slot 47 excluded — replaced by spectate browser button.
    private static final int[] BLACK_SLOTS   = {1, 3, 5, 7, 9, 11, 13, 17, 21, 27, 29, 31, 35, 37, 39, 49, 53};
    private static final int[] SESSION_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
    private static final String CREATE_PERMISSION = "skript.game.create_session";

    private static MainGuiService instance;
    private final Set<UUID> activeViewers = new HashSet<>();
    private final java.util.Map<UUID, String> viewerFilters = new ConcurrentHashMap<>();
    private final Set<UUID> awaitingFilterInput = ConcurrentHashMap.newKeySet();
    private final java.util.Map<UUID, Set<MinigameTag>> viewerTagFilters = new ConcurrentHashMap<>();

    private MainGuiService() {}

    public static synchronized MainGuiService getInstance() {
        if (instance == null) instance = new MainGuiService();
        return instance;
    }

    public void openFor(Player viewer) {
        MainGuiOpenEvent guiEvent = new MainGuiOpenEvent(viewer);
        Bukkit.getPluginManager().callEvent(guiEvent);
        if (guiEvent.isCancelled()) return;
        viewer.openInventory(buildFor(viewer));
        activeViewers.add(viewer.getUniqueId());
    }

    public void update() {
        for (UUID uuid : new HashSet<>(activeViewers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                activeViewers.remove(uuid);
                continue;
            }
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) {
                openFor(p);
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

    @EventHandler
    public void onSessionCreate(SessionCreateEvent event) {
        // SessionCreateEvent fires before setHost() in SessionLifecycleManagerImpl — defer by one tick
        Bukkit.getScheduler().runTask(SkGame.getInstance(), this::update);
    }

    @EventHandler
    public void onSessionDisband(SessionDisbandEvent event) {
        // Defer by one tick so sessionManager.deleteSession() completes before we rebuild.
        Bukkit.getScheduler().runTask(SkGame.getInstance(), this::update);
    }

    @EventHandler
    public void onGameStart(GameStartEvent event) { update(); }

    @EventHandler
    public void onGameStop(GameStopEvent event) { update(); }

    // Both chat events cancelled so CMI (which uses legacy AsyncPlayerChatEvent) also suppressed.
    // setCancelled + clear happen in both handlers; processFilterInput uses remove() for idempotency.

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPaperChat(AsyncChatEvent event) {
        Player p = event.getPlayer();
        if (!awaitingFilterInput.contains(p.getUniqueId())) return;
        event.setCancelled(true);
        event.viewers().clear();
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        processFilterInput(p, text);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    @SuppressWarnings("deprecation")
    public void onLegacyChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        if (!awaitingFilterInput.contains(p.getUniqueId())) return;
        event.setCancelled(true);
        event.getRecipients().clear();
        processFilterInput(p, event.getMessage().trim());
    }

    private void processFilterInput(Player p, String text) {
        if (!awaitingFilterInput.remove(p.getUniqueId())) return;
        if (text.equalsIgnoreCase("clear") || text.isEmpty()) {
            viewerFilters.remove(p.getUniqueId());
        } else {
            viewerFilters.put(p.getUniqueId(), text);
        }
        Bukkit.getScheduler().runTask(SkGame.getInstance(), () -> openFor(p));
    }

    // ─── Filter API (used by ExprMainGuiFilter Skript expression) ────────────

    public @Nullable String getFilter(Player player) {
        return viewerFilters.get(player.getUniqueId());
    }

    public void setFilter(Player player, @Nullable String filter) {
        if (filter == null || filter.isEmpty()) {
            viewerFilters.remove(player.getUniqueId());
        } else {
            viewerFilters.put(player.getUniqueId(), filter);
        }
    }

    public Set<MinigameTag> getTagFilter(Player player) {
        return viewerTagFilters.getOrDefault(player.getUniqueId(), Set.of());
    }

    public void setTagFilter(Player player, Set<MinigameTag> tags) {
        if (tags == null || tags.isEmpty()) {
            viewerTagFilters.remove(player.getUniqueId());
        } else {
            viewerTagFilters.put(player.getUniqueId(), new HashSet<>(tags));
        }
    }

    private Inventory buildFor(Player viewer) {
        SessionLifecycleManagerImpl lifecycle = SessionLifecycleManagerImpl.getInstance();
        SessionManager sm = SessionManager.getInstance();

        GuiItem grayGlass  = GuiItem.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.space());
        GuiItem redGlass   = GuiItem.of(Material.RED_STAINED_GLASS_PANE).name(Component.space());
        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space());

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(Messages.getComponent("gui.main.title", viewer));

        for (int s : GRAY_SLOTS)  builder.slot(s, grayGlass);
        for (int s : RED_SLOTS)   builder.slot(s, redGlass);
        for (int s : BLACK_SLOTS) builder.slot(s, blackGlass);

        // Slot 6 — Create session (permission: skript.game.create_session)
        builder.slot(6, GuiItem.of(Material.LIME_STAINED_GLASS_PANE)
                .name("&a&lCreate session")
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (sm.getSession(p) != null) {
                        Messages.send(p, "session.error.already-in-session");
                        return;
                    }
                    if (!p.hasPermission(CREATE_PERMISSION)) {
                        Messages.send(p, "command.error.no-permission");
                        return;
                    }
                    Session created = lifecycle.createSession(p);
                    if (created != null) {
                        SessionGuiService.getInstance().openFor(p);
                    }
                }));

        // Slot 19 — Leaderboards
        if (DatabaseManager.getInstance().isAvailable()) {
            builder.slot(19, GuiItem.of(Material.NETHER_STAR)
                    .name(Messages.getComponent("gui.leaderboard.button.title", viewer))
                    .lore(Messages.getComponent("gui.leaderboard.button.lore", viewer))
                    .onClick(e -> LeaderboardGuiService.getInstance().openMinigamePicker((Player) e.getWhoClicked())));
        } else {
            builder.slot(19, GuiItem.of(Material.BARRIER)
                    .name(Messages.getComponent("gui.leaderboard.button.unavailable", viewer)));
        }

        // Slot 45 — Close
        builder.slot(45, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lClose")
                .onClick(e -> e.getWhoClicked().closeInventory()));

        // Slot 47 — Spectate browser
        builder.slot(47, GuiItem.of(Material.SPYGLASS)
                .name("&3&lSpectate running games")
                .lore(legacy("&7Browse and join active games"))
                .onClick(e -> SpectateGuiService.getInstance().openFor((Player) e.getWhoClicked())));

        // Slot 46 — Your profile
        builder.slot(46, GuiItem.of(Material.PLAYER_HEAD)
                .name("&3&lYour profile")
                .lore(legacy("&7View your game statistics"))
                .onClick(e -> PlayerProfileGuiService.getInstance().openFor((Player) e.getWhoClicked(), (Player) e.getWhoClicked())));

        // Slot 51 — Open current session
        builder.slot(51, GuiItem.of(Material.BOOK)
                .name("&a&lOpen current session")
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (sm.getSession(p) != null) {
                        SessionGuiService.getInstance().openFor(p);
                    }
                    // else: player not in session — no-op (matches .sk implicit stop)
                }));

        // Slot 4 — Filter sessions (overrides the red glass pane from RED_SLOTS)
        String currentFilter = viewerFilters.get(viewer.getUniqueId());
        Set<MinigameTag> currentTagFilter = viewerTagFilters.getOrDefault(viewer.getUniqueId(), Set.of());
        boolean hasTextFilter = currentFilter != null && !currentFilter.isEmpty();
        boolean hasTagFilter = !currentTagFilter.isEmpty();
        boolean hasAnyFilter = hasTextFilter || hasTagFilter;

        List<Component> filterLore = new java.util.ArrayList<>();
        if (hasTextFilter) filterLore.add(legacy("&7Text: &f" + currentFilter));
        if (hasTagFilter) {
            String tagStr = currentTagFilter.stream()
                    .map(MinigameTag::displayName)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            filterLore.add(legacy("&7Tags: &f" + tagStr));
        }
        filterLore.add(legacy("&aLeft-click &7to pick tags"));
        filterLore.add(legacy("&eShift-click &7to type text filter"));
        if (hasAnyFilter) filterLore.add(legacy("&cRight-click &7to clear all"));

        builder.slot(4, GuiItem.of(Material.HOPPER)
                .name(hasAnyFilter ? "&e&lFiltered" : "&e&lFilter sessions")
                .lore(filterLore)
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (e.getClick().isRightClick()) {
                        viewerFilters.remove(p.getUniqueId());
                        viewerTagFilters.remove(p.getUniqueId());
                        openFor(p);
                        return;
                    }
                    if (e.getClick().isShiftClick()) {
                        p.closeInventory();
                        awaitingFilterInput.add(p.getUniqueId());
                        p.sendMessage(legacy("&eType a filter (minigame, host, or map name). Type &cclear &eto reset:"));
                        return;
                    }
                    // Left-click — open tag picker
                    Set<MinigameTag> activeTags = viewerTagFilters.getOrDefault(p.getUniqueId(), Set.of());
                    FilterPickerGuiService.getInstance().openFor(p, activeTags);
                }));

        // Dynamic session list — LOBBY + public only, filtered, sorted oldest-first
        String filter = viewerFilters.get(viewer.getUniqueId());
        Set<MinigameTag> tagFilter = viewerTagFilters.getOrDefault(viewer.getUniqueId(), Set.of());
        List<Session> lobbySessions = Arrays.stream(sm.getAllSessions())
                .filter(s -> s.getState() == SessionState.LOBBY
                        && s.getVisibility() == SessionVisibility.PUBLIC)
                .filter(s -> matchesFilter(s, filter, tagFilter))
                .sorted(Comparator.comparingLong(Session::getCreatedAt))
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(lobbySessions.size(), SESSION_SLOTS.length); i++) {
            builder.slot(SESSION_SLOTS[i], buildSessionItem(lobbySessions.get(i), lifecycle, sm));
        }

        return builder.build();
    }

    private GuiItem buildSessionItem(Session session, SessionLifecycleManagerImpl lifecycle,
                                     SessionManager sm) {
        Player host = session.getHost();
        String hostName = host != null ? host.getName() : "?";
        String sessionId = session.getId();

        List<Component> lore = session.getLobbyMembers().stream()
                .map(p -> legacy("&7" + p.getName()))
                .collect(Collectors.toList());

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (host != null) {
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setPlayerProfile(host.getPlayerProfile());
                skull.setItemMeta(meta);
            }
        }

        return GuiItem.of(skull)
                .name("&3" + hostName)
                .lore(lore)
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session clicked = sm.getSessionById(sessionId);
                    if (clicked == null) return;

                    Session cur = sm.getSession(p);
                    if (cur == null) {
                        boolean joined = lifecycle.joinSession(p, clicked);
                        if (joined) {
                            SessionGuiService.getInstance().openFor(p);
                        }
                    } else if (cur.getId().equals(sessionId)) {
                        SessionGuiService.getInstance().openFor(p);
                    } else {
                        Messages.send(p, "session.error.already-in-session");
                    }
                });
    }

    private boolean matchesFilter(Session s, @Nullable String textFilter, Set<MinigameTag> tagFilter) {
        // AND between dimensions: both conditions must pass when both are active
        if (textFilter != null && !textFilter.isEmpty()) {
            String f = textFilter.toLowerCase(Locale.ROOT);
            boolean textMatch = false;
            if (s.getHost() != null && s.getHost().getName().toLowerCase(Locale.ROOT).contains(f))
                textMatch = true;
            if (!textMatch && s.getMiniGame() != null) {
                Object mgName = s.getMiniGame().getValue("name");
                String mgStr = mgName != null ? mgName.toString() : s.getMiniGame().getId();
                if (mgStr.toLowerCase(Locale.ROOT).contains(f)) textMatch = true;
            }
            if (!textMatch && s.getGameMap() != null
                    && s.getGameMap().getId().toLowerCase(Locale.ROOT).contains(f))
                textMatch = true;
            if (!textMatch) return false;
        }
        // Tag filter: OR within set (session must have at least one matching tag)
        if (!tagFilter.isEmpty()) {
            if (s.getMiniGame() == null) return false;
            Set<MinigameTag> sessionTags = s.getMiniGame().getTags();
            boolean tagMatch = false;
            for (MinigameTag t : tagFilter) {
                if (sessionTags.contains(t)) { tagMatch = true; break; }
            }
            if (!tagMatch) return false;
        }
        return true;
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
