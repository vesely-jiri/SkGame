package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.type.CustomValuePlurality;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.core.admin.AdminSetupState;
import cz.nox.skgame.core.admin.AdminWand;
import cz.nox.skgame.core.admin.BoundaryPreview;
import cz.nox.skgame.core.admin.LocationBeam;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.region.CuboidRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import cz.nox.skgame.api.gui.event.AdminGuiOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class AdminGuiService implements Listener {

    private static final String ADMIN_PREFIX = "§2<Game Admin> §r";

    private static final int[] BLACK_BORDER = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36, 44,
            45, 46, 47, 48, 49, 50, 51, 52
    };
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static AdminGuiService instance;
    private final Map<UUID, AdminSetupState> states = new HashMap<>();
    private final AdminWand wand;

    private AdminGuiService() {
        this.wand = new AdminWand(new NamespacedKey(SkGame.getInstance(), "wand"));
    }

    public static synchronized AdminGuiService getInstance() {
        if (instance == null) instance = new AdminGuiService();
        return instance;
    }

    /** Number of admins currently in wand-setup mode (each runs a 1t LocationBeam task). */
    public int getActiveSetupSessionCount() { return states.size(); }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void openAdminGui(Player player) {
        AdminGuiOpenEvent guiEvent = new AdminGuiOpenEvent(player);
        Bukkit.getPluginManager().callEvent(guiEvent);
        if (guiEvent.isCancelled()) return;
        GameMapManager gmm = GameMapManager.getInstance();

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(legacy("&cAdmin gui"));

        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.text(" "));
        for (int s : BLACK_BORDER) builder.slot(s, blackGlass);

        builder.slot(4, GuiItem.of(Material.STONE_BUTTON)
                .name("&eSet lobby location")
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    SkGame.getInstance().setLobbySpawn(p.getLocation());
                    p.sendMessage(ADMIN_PREFIX + "Lobby location set.");
                }));

        builder.slot(45, GuiItem.of(Material.GOLDEN_AXE)
                .name("&6Get wand")
                .onClick(e -> giveWand((Player) e.getWhoClicked())));

        builder.slot(49, GuiItem.of(Material.LIME_STAINED_GLASS_PANE)
                .name("&aCreate map")
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    states.computeIfAbsent(p.getUniqueId(), k -> new AdminSetupState())
                            .setResponseMode(AdminSetupState.ResponseMode.MAP_CREATION);
                    p.closeInventory();
                    p.sendMessage(ADMIN_PREFIX + "Select two points with the wand, then type the map name in chat:");
                }));

        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lClose gui")
                .onClick(e -> stopSetupMode((Player) e.getWhoClicked())));

        // Dynamic map list
        GameMap[] maps = gmm.getGameMaps();
        for (int i = 0; i < Math.min(maps.length, ITEM_SLOTS.length); i++) {
            GameMap map = maps[i];
            String mapId = map.getId();
            Object nameObj = map.getValue("name");
            String displayName = nameObj != null ? nameObj.toString() : mapId;

            builder.slot(ITEM_SLOTS[i], GuiItem.of(Material.FILLED_MAP)
                    .name("&6" + displayName)
                    .lore(
                            legacy("&7Left-click: &aOpen settings"),
                            legacy("&7Shift+Right-click: &cDelete map")
                    )
                    .onLeftClick(e -> openMapPropertiesGui((Player) e.getWhoClicked(), mapId))
                    .onShiftClick(e -> {
                        if (e.getClick() != ClickType.SHIFT_RIGHT) return;
                        gmm.unregisterGameMap(mapId);
                        openAdminGui((Player) e.getWhoClicked());
                    }));
        }

        player.openInventory(builder.build());
    }

    public void openMapPropertiesGui(Player player, String mapId) {
        GameMap map = GameMapManager.getInstance().getGameMapById(mapId);
        if (map == null) return;
        MiniGame[] minigames = MiniGameManager.getInstance().getAllMiniGames();

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(legacy("&9Map Minigames"));

        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.text(" "));
        for (int s : BLACK_BORDER) builder.slot(s, blackGlass);

        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lBack to admin menu")
                .onClick(e -> openAdminGui((Player) e.getWhoClicked())));

        for (int i = 0; i < Math.min(minigames.length, ITEM_SLOTS.length); i++) {
            MiniGame mg = minigames[i];
            String mgId = mg.getId();
            boolean assigned = map.supportsMiniGame(mg);

            Object nameObj = mg.getValue("name");
            String displayName = nameObj != null ? nameObj.toString() : mgId;
            Material mat = assigned ? Material.LIME_WOOL : Material.GRAY_WOOL;

            builder.slot(ITEM_SLOTS[i], GuiItem.of(mat)
                    .name((assigned ? "&a" : "&7") + displayName)
                    .lore(
                            legacy(assigned ? "&2Assigned" : "&7Unassigned"),
                            legacy("&7Left-click: &aOpen map settings"),
                            legacy("&7Shift+Right-click: &aAssign&7/&cUnassign")
                    )
                    .onLeftClick(e -> openValuesGui((Player) e.getWhoClicked(), mapId, mgId))
                    .onShiftClick(e -> {
                        if (e.getClick() != ClickType.SHIFT_RIGHT) return;
                        toggleAssignMiniGame(map, mg);
                        openMapPropertiesGui((Player) e.getWhoClicked(), mapId);
                    }));
        }

        player.openInventory(builder.build());
    }

    public void openValuesGui(Player player, String mapId, String mgId) {
        GameMap map = GameMapManager.getInstance().getGameMapById(mapId);
        MiniGame mg = MiniGameManager.getInstance().getMiniGameById(mgId);
        if (map == null || mg == null) return;

        AdminSetupState state = states.computeIfAbsent(player.getUniqueId(), k -> new AdminSetupState());
        state.setCurrentMapId(mapId);
        state.setCurrentMiniGameId(mgId);

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(legacy("&9Map Values &7(&e" + mgId + "&7)"));

        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.text(" "));
        for (int s : BLACK_BORDER) builder.slot(s, blackGlass);

        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lBack to map properties")
                .onClick(e -> openMapPropertiesGui((Player) e.getWhoClicked(), mapId)));

        int idx = 0;
        for (Map.Entry<String, CustomValue> entry : mg.getGameMapValueDefs().entrySet()) {
            if (idx >= ITEM_SLOTS.length) break;
            String key = entry.getKey();
            CustomValue cvDef = entry.getValue();

            Object rawMapVal = map.getMiniGameValue(mgId, key);
            String typeStr = (cvDef.getType() != null) ? cvDef.getType().getCodeName() : "unknown";
            String plurStr = cvDef.getPlurality().name();
            boolean valueIsPlural = cvDef.getPlurality() == CustomValuePlurality.PLURAL;
            boolean isRegion = "skgameregion".equals(typeStr);
            boolean isLocation = "location".equals(typeStr);

            String valueDisplay;
            if (rawMapVal == null) {
                valueDisplay = "&cNot set";
            } else if (isRegion) {
                valueDisplay = "&aConfigured";
            } else if (rawMapVal instanceof Object[] arr) {
                valueDisplay = "&6List (" + arr.length + ")";
            } else {
                valueDisplay = formatValue(rawMapVal);
            }

            String action;
            if (isRegion) action = "&aSet region (wand)";
            else if (valueIsPlural) action = "&aOpen value list";
            else if (isLocation) action = "&aSet location (picker)";
            else if (cvDef.hasAllowedValues()) action = "&aChoose from options";
            else action = "&aSet value";

            Material mat = rawMapVal != null ? Material.CHEST_MINECART : Material.MINECART;
            String nameColor = rawMapVal != null ? "&a" : "&c";

            builder.slot(ITEM_SLOTS[idx++], GuiItem.of(mat)
                    .name(nameColor + key)
                    .lore(
                            legacy("&eType: " + typeStr),
                            legacy("&6Value: " + valueDisplay),
                            legacy("&3Plurality: " + plurStr),
                            legacy("&7---------"),
                            legacy("&7Left-click: " + action),
                            legacy("&7Shift+Right-click: &cDelete value")
                    )
                    .onLeftClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        AdminSetupState st = states.computeIfAbsent(p.getUniqueId(), k -> new AdminSetupState());
                        if (isRegion) {
                            st.setResponseMode(AdminSetupState.ResponseMode.REGION_INPUT);
                            st.setCurrentMapId(mapId);
                            st.setCurrentMiniGameId(mgId);
                            st.setCurrentValueKey(key);
                            p.closeInventory();
                            p.sendMessage(ADMIN_PREFIX + "Select two corners with the wand, then type 'save' in chat:");
                        } else if (valueIsPlural) {
                            openPluralListGui(p, mapId, mgId, key);
                        } else if (isLocation) {
                            p.closeInventory();
                            sendLocationPicker(p, mapId, mgId, key, true);
                        } else if (cvDef.hasAllowedValues()) {
                            openEnumPickerGui(p, mapId, mgId, key, cvDef.getAllowedValues());
                        } else {
                            st.setResponseMode(AdminSetupState.ResponseMode.VALUE_INPUT);
                            st.setCurrentMapId(mapId);
                            st.setCurrentMiniGameId(mgId);
                            st.setCurrentValueKey(key);
                            p.closeInventory();
                            p.sendMessage(ADMIN_PREFIX + "Type the new value in chat (or 'cancel' to abort):");
                        }
                    })
                    .onShiftClick(e -> {
                        if (e.getClick() != ClickType.SHIFT_RIGHT) return;
                        GameMap freshMap = GameMapManager.getInstance().getGameMapById(mapId);
                        if (freshMap != null) {
                            freshMap.setMiniGameValue(mgId, key, null);
                            GameMapManager.getInstance().save();
                        }
                        openValuesGui((Player) e.getWhoClicked(), mapId, mgId);
                    }));
        }

        player.openInventory(builder.build());
    }

    public void openPluralListGui(Player player, String mapId, String mgId, String key) {
        GameMap map = GameMapManager.getInstance().getGameMapById(mapId);
        MiniGame mg = MiniGameManager.getInstance().getMiniGameById(mgId);
        if (map == null || mg == null) return;

        AdminSetupState state = states.computeIfAbsent(player.getUniqueId(), k -> new AdminSetupState());
        state.setCurrentMapId(mapId);
        state.setCurrentMiniGameId(mgId);
        state.setCurrentValueKey(key);

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(legacy("&9Edit Value &7(&e" + key + "&7)"));

        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.text(" "));
        for (int s : BLACK_BORDER) builder.slot(s, blackGlass);

        builder.slot(49, GuiItem.of(Material.LIME_STAINED_GLASS_PANE)
                .name("&aAdd new value")
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    p.closeInventory();
                    sendLocationPicker(p, mapId, mgId, key, false);
                }));

        builder.slot(50, GuiItem.of(Material.ENDER_EYE)
                .name("&aShow all locations")
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    GameMap freshMap = GameMapManager.getInstance().getGameMapById(mapId);
                    if (freshMap != null) showLocationsForKey(freshMap, mgId, key);
                    p.closeInventory();
                    SkGame plugin = SkGame.getInstance();
                    Component back = Component.text(ADMIN_PREFIX).append(
                            Component.text("[Back to menu]").color(NamedTextColor.AQUA)
                                    .clickEvent(ClickEvent.callback(a -> {
                                        if (!(a instanceof Player bp)) return;
                                        Bukkit.getScheduler().runTask(plugin,
                                                () -> openPluralListGui(bp, mapId, mgId, key));
                                    })));
                    p.sendMessage(back);
                }));

        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lBack to minigame values")
                .onClick(e -> openValuesGui((Player) e.getWhoClicked(), mapId, mgId)));

        // Dynamic plural value list
        Object rawVal = map.getMiniGameValue(mgId, key);
        if (rawVal instanceof Object[] arr) {
            SkGame plugin = SkGame.getInstance();
            for (int i = 0; i < Math.min(arr.length, ITEM_SLOTS.length); i++) {
                Object valueObj = arr[i];
                builder.slot(ITEM_SLOTS[i], GuiItem.of(Material.CHEST)
                        .name("&e" + formatValue(valueObj))
                        .lore(
                                legacy("&7Left-click: &aShow location"),
                                legacy("&7Right-click: &cDelete value")
                        )
                        .onLeftClick(e -> {
                            Player p = (Player) e.getWhoClicked();
                            if (valueObj instanceof Location loc) {
                                new LocationBeam(loc, 200, plugin).spawn();
                            }
                            p.closeInventory();
                            Component back = Component.text(ADMIN_PREFIX).append(
                                    Component.text("[Back to menu]").color(NamedTextColor.AQUA)
                                            .clickEvent(ClickEvent.callback(a -> {
                                                if (!(a instanceof Player bp)) return;
                                                Bukkit.getScheduler().runTask(plugin,
                                                        () -> openPluralListGui(bp, mapId, mgId, key));
                                            })));
                            p.sendMessage(back);
                        })
                        .onRightClick(e -> {
                            // Fix for .sk bug: original removes "t" instead of actual value
                            GameMap freshMap = GameMapManager.getInstance().getGameMapById(mapId);
                            if (freshMap != null) freshMap.removeMiniGameValue(mgId, key, valueObj);
                            openPluralListGui((Player) e.getWhoClicked(), mapId, mgId, key);
                        }));
            }
        }

        player.openInventory(builder.build());
    }

    public void openEnumPickerGui(Player player, String mapId, String mgId, String key, List<String> allowedValues) {
        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(legacy("&3Choose value for " + key));

        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.text(" "));
        for (int s : BLACK_BORDER) builder.slot(s, blackGlass);

        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lBack to values")
                .onClick(e -> openValuesGui((Player) e.getWhoClicked(), mapId, mgId)));

        List<String> vals = new ArrayList<>(allowedValues);
        for (int i = 0; i < Math.min(vals.size(), ITEM_SLOTS.length); i++) {
            String val = vals.get(i);
            builder.slot(ITEM_SLOTS[i], GuiItem.of(Material.PAPER)
                    .name("&f" + val)
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        GameMap freshMap = GameMapManager.getInstance().getGameMapById(mapId);
                        if (freshMap != null) {
                            freshMap.setMiniGameValue(mgId, key, val);
                            GameMapManager.getInstance().save();
                        }
                        openValuesGui(p, mapId, mgId);
                    }));
        }

        player.openInventory(builder.build());
    }

    public void giveWand(Player player) {
        states.computeIfAbsent(player.getUniqueId(), k -> new AdminSetupState());
        for (ItemStack item : player.getInventory().getContents()) {
            if (wand.isWand(item)) return; // already has one
        }
        player.getInventory().addItem(wand.create());
        player.sendMessage(ADMIN_PREFIX + "Wand given. Left-click = pos1, right-click = pos2.");
    }

    public void stopSetupMode(Player player) {
        AdminSetupState state = states.remove(player.getUniqueId());
        if (state != null) state.clearPositions();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (wand.isWand(item)) player.getInventory().setItem(i, null);
        }
        player.closeInventory();
        player.sendMessage(ADMIN_PREFIX + "Setup mode disabled.");
    }

    // ─── Event handlers ───────────────────────────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (!wand.isWand(e.getItem())) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        Player player = e.getPlayer();
        if (!states.containsKey(player.getUniqueId())) return;

        e.setCancelled(true);
        AdminSetupState state = states.get(player.getUniqueId());
        Location loc = block.getLocation();

        if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
            state.setPos1(loc);
            player.sendMessage(ADMIN_PREFIX + "Position 1 set to " + formatLoc(loc));
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            state.setPos2(loc);
            player.sendMessage(ADMIN_PREFIX + "Position 2 set to " + formatLoc(loc));
        }
        // Refresh preview after every corner change (starts as soon as pos1 is set)
        if (state.getPos1() != null) {
            BoundaryPreview preview = new BoundaryPreview(
                    player, state.getPos1(), state.getPos2(), SkGame.getInstance());
            state.setBoundaryPreview(preview);
            preview.start();
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        AdminSetupState state = states.get(e.getPlayer().getUniqueId());
        if (state == null || state.getResponseMode() == AdminSetupState.ResponseMode.NONE) return;

        e.setCancelled(true);
        Player player = e.getPlayer();
        String message = e.getMessage();

        if ("cancel".equalsIgnoreCase(message)) {
            state.setResponseMode(AdminSetupState.ResponseMode.NONE);
            state.clearPositions();
            player.sendMessage(ADMIN_PREFIX + "Action cancelled.");
            return;
        }

        AdminSetupState.ResponseMode mode = state.getResponseMode();
        Bukkit.getScheduler().runTask(SkGame.getInstance(), () -> {
            if (mode == AdminSetupState.ResponseMode.MAP_CREATION) {
                createMap(player, message, state);
            } else if (mode == AdminSetupState.ResponseMode.VALUE_INPUT) {
                setMapValue(player, message, state);
            } else if (mode == AdminSetupState.ResponseMode.REGION_INPUT) {
                if ("save".equalsIgnoreCase(message)) {
                    saveRegionValue(player, state);
                } else {
                    player.sendMessage(ADMIN_PREFIX + "Use the wand to select corners, then type 'save'. Type 'cancel' to abort.");
                }
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        AdminSetupState state = states.remove(e.getPlayer().getUniqueId());
        if (state != null) state.clearPositions();
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!wand.isWand(e.getItemDrop().getItemStack())) return;
        e.setCancelled(true);
        stopSetupMode(e.getPlayer());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void createMap(Player player, String name, AdminSetupState state) {
        if (!state.hasRegion()) {
            player.sendMessage(ADMIN_PREFIX + "§cSet both positions with the wand first.");
            return;
        }
        GameMapManager gmm = GameMapManager.getInstance();
        if (gmm.isMapRegistered(name)) {
            player.sendMessage(ADMIN_PREFIX + "§cMap '" + name + "' already exists.");
            return;
        }
        GameMap map = gmm.registerGameMap(name);
        map.setValue("name", name);
        map.setRegion(new CuboidRegion(state.getPos1(), state.getPos2()));
        state.setResponseMode(AdminSetupState.ResponseMode.NONE);
        state.clearPositions();
        player.sendMessage(ADMIN_PREFIX + "Map '" + name + "' created.");
        openAdminGui(player);
    }

    private void setMapValue(Player player, String message, AdminSetupState state) {
        String mapId = state.getCurrentMapId();
        String mgId = state.getCurrentMiniGameId();
        String key = state.getCurrentValueKey();
        if (mapId == null || mgId == null || key == null) return;
        GameMap map = GameMapManager.getInstance().getGameMapById(mapId);
        if (map == null) return;
        Object parsed = parseTypedValue(message);
        MiniGame mg = MiniGameManager.getInstance().getMiniGameById(mgId);
        if (mg != null) {
            CustomValue def = mg.getGameMapValueDef(key);
            if (def != null && def.hasBounds()) {
                Object clamped = def.clamp(parsed);
                if (clamped != parsed) {
                    player.sendMessage(ADMIN_PREFIX + "§eValue clamped to bounds [" + def.getMinValue() + ", " + def.getMaxValue() + "].");
                    parsed = clamped;
                }
            }
        }
        map.setMiniGameValue(mgId, key, parsed);
        GameMapManager.getInstance().save();
        state.setResponseMode(AdminSetupState.ResponseMode.NONE);
        player.sendMessage(ADMIN_PREFIX + "Value '" + key + "' set to " + parsed);
        openValuesGui(player, mapId, mgId);
    }

    private void saveRegionValue(Player player, AdminSetupState state) {
        if (!state.hasRegion()) {
            player.sendMessage(ADMIN_PREFIX + "§cSet both positions with the wand first.");
            return;
        }
        String mapId = state.getCurrentMapId();
        String mgId = state.getCurrentMiniGameId();
        String key = state.getCurrentValueKey();
        if (mapId == null || mgId == null || key == null) return;
        GameMap map = GameMapManager.getInstance().getGameMapById(mapId);
        if (map == null) return;
        map.setMiniGameValue(mgId, key, new CuboidRegion(state.getPos1(), state.getPos2()));
        GameMapManager.getInstance().save();
        state.setResponseMode(AdminSetupState.ResponseMode.NONE);
        state.clearPositions();
        player.sendMessage(ADMIN_PREFIX + "Region value '" + key + "' saved.");
        openValuesGui(player, mapId, mgId);
    }

    private void toggleAssignMiniGame(GameMap map, MiniGame mg) {
        if (map.supportsMiniGame(mg)) {
            map.setMiniGameValues(mg.getId(), null);
        } else {
            map.setMiniGameValue(mg.getId(), "enabled", true);
            for (Map.Entry<String, CustomValue> entry : mg.getGameMapValueDefs().entrySet()) {
                Object toSet = entry.getValue().getDefaultValue();
                if (toSet != null) map.setMiniGameValue(mg.getId(), entry.getKey(), toSet);
            }
        }
        GameMapManager.getInstance().save();
    }

    private void sendLocationPicker(Player player, String mapId, String mgId, String key, boolean single) {
        SkGame plugin = SkGame.getInstance();
        ClickCallback.Options oneUse = ClickCallback.Options.builder().uses(1).build();

        Component msg = Component.text(ADMIN_PREFIX + "Save location of: ").color(NamedTextColor.YELLOW)
                .append(Component.text("[Targeted block]").color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.callback(a -> {
                            if (!(a instanceof Player p)) return;
                            Block target = p.getTargetBlockExact(10);
                            if (target == null) { p.sendMessage(ADMIN_PREFIX + "§cNo block in sight."); return; }
                            Location loc = target.getLocation();
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                GameMap m = GameMapManager.getInstance().getGameMapById(mapId);
                                if (m != null) {
                                    if (single) m.setMiniGameValue(mgId, key, loc);
                                    else m.addMiniGameValue(mgId, key, loc);
                                    GameMapManager.getInstance().save();
                                }
                                if (single) openValuesGui(p, mapId, mgId);
                                else openPluralListGui(p, mapId, mgId, key);
                            });
                        }, oneUse)))
                .append(Component.text(" [Player]").color(NamedTextColor.LIGHT_PURPLE)
                        .clickEvent(ClickEvent.callback(a -> {
                            if (!(a instanceof Player p)) return;
                            Location loc = p.getLocation();
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                GameMap m = GameMapManager.getInstance().getGameMapById(mapId);
                                if (m != null) {
                                    if (single) m.setMiniGameValue(mgId, key, loc);
                                    else m.addMiniGameValue(mgId, key, loc);
                                    GameMapManager.getInstance().save();
                                }
                                if (single) openValuesGui(p, mapId, mgId);
                                else openPluralListGui(p, mapId, mgId, key);
                            });
                        }, oneUse)))
                .append(Component.text(" [Stabilize]").color(NamedTextColor.DARK_AQUA)
                        .clickEvent(ClickEvent.callback(a -> {
                            if (!(a instanceof Player p)) return;
                            Bukkit.getScheduler().runTask(plugin, () -> stabilizePlayer(p));
                        })))
                .append(Component.text(" [Back to menu]").color(NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.callback(a -> {
                            if (!(a instanceof Player p)) return;
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (single) openValuesGui(p, mapId, mgId);
                                else openPluralListGui(p, mapId, mgId, key);
                            });
                        })));
        player.sendMessage(msg);
    }

    private void showLocationsForKey(GameMap map, String mgId, String key) {
        Object rawVal = map.getMiniGameValue(mgId, key);
        if (!(rawVal instanceof Object[] arr)) return;
        SkGame plugin = SkGame.getInstance();
        for (Object o : arr) {
            if (o instanceof Location loc) new LocationBeam(loc, 200, plugin).spawn();
        }
    }

    private void stabilizePlayer(Player player) {
        Location loc = player.getLocation();
        loc.setX(loc.getBlockX() + 0.5);
        loc.setZ(loc.getBlockZ() + 0.5);
        loc.setPitch(0f);
        loc.setYaw(switch (player.getFacing()) {
            case SOUTH -> 0f;
            case WEST  -> 90f;
            case NORTH -> 180f;
            default    -> -90f;
        });
        player.teleport(loc);
    }

    private Object parseTypedValue(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        if ("true".equalsIgnoreCase(s)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(s)) return Boolean.FALSE;
        return s;
    }

    private String formatValue(Object o) {
        if (o instanceof Location loc) {
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
            return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ") in " + world;
        }
        return o.toString();
    }

    private String formatLoc(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
