package cz.nox.skgame.api.game.model;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.GamePlayerSessionJoin;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.PlayerRoleChangeEvent;
import cz.nox.skgame.api.game.model.type.MapSelectionMode;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.region.Region;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.region.ArenaSlot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.LinkedHashSet;

public class Session {
    private String id;
    private final long createdAt = System.currentTimeMillis();
    private long startedAt = 0L;
    private Player host;
    private final LinkedHashSet<Player> lobbyMembers = new LinkedHashSet<>();
    private HashSet<Player> players;
    private HashSet<Player> spectators;
    private SessionState state;
    private MiniGame miniGame;
    private GameMap gameMap;
    private Map<String, Object> values;
    private Map<String, Object> tempValues;
    @Nullable private ArenaSlot claimedSlot;
    @Nullable private Region arenaRegion;
    private int totalRounds = 1;
    private int currentRound = 0;
    private boolean allowSpectate = SkGame.getInstance().getSpectateDefaultAllow();
    private boolean shuffle = false;
    private SessionVisibility visibility = SessionVisibility.PUBLIC;
    private List<Player> winners = new ArrayList<>();
    private final Set<UUID> invitedPlayers = new HashSet<>();
    @Nullable private String joinCode;
    private final Map<UUID, String> teamAssignments = new HashMap<>();
    private MapSelectionMode mapSelectionMode = MapSelectionMode.SPECIFIC;
    private boolean persistent = false;
    private boolean eventSession = false;
    private final Map<UUID, String> mapVotes = new HashMap<>();
    /** Transient per-session bans. UUID → name captured at ban time (no OfflinePlayer lookups needed). */
    private final java.util.LinkedHashMap<UUID, String> bannedPlayers = new java.util.LinkedHashMap<>();
    /** Admin-queued config changes to apply at round end. Flushed in runDeferredBlock before round logic. */
    private final Map<String, Object> pendingAdminChanges = new HashMap<>();

    public Session(String id, Player host, MiniGame miniGame,
                   SessionState state, GameMap map, Set<Player> players, Set<Player> spectators,
                   Map<String, Object> values, Map<String, Object> tempValues) {
        this.id = id;
        this.host = host;
        this.players = new HashSet<>(players);
        this.spectators = new HashSet<>(spectators);
        this.state = state;
        this.miniGame = miniGame;
        this.gameMap = map;
        this.values = new HashMap<>(values);
        this.tempValues = new HashMap<>(tempValues);
    }

    public Session(String id) {
        this(id,null,null,SessionState.LOBBY,null,
                new HashSet<>(),new HashSet<>(),new HashMap<>(),new HashMap<>());
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }


    public Player getHost() {
        return host;
    }
    public void setHost(Player host) {
        this.host = host;
    }

    public HashSet<Player> getPlayers() {
        return new HashSet<>(players);
    }
    public void addPlayers(Player... players) {
        for (Player player : players) {
            if (this.players.add(player)) {
                Bukkit.getPluginManager().callEvent(new GamePlayerSessionJoin(player,this));
            }
        }
    }
    public void removePlayers(Player... players) {
        for (Player player : players) {
            if (this.players.remove(player)) {
                Bukkit.getPluginManager().callEvent(new GamePlayerSessionLeave(player, this));
            }
        }
    }
    public HashSet<Player> getSpectators() {
        return new HashSet<>(spectators);
    }
    public void addSpectators(Player... spectators) {
        this.spectators.addAll(Arrays.asList(spectators));
    }
    public void removeSpectators(Player... spectators) {
        for (Player spectator : spectators) {
            this.spectators.remove(spectator);
        }
    }

    public Set<Player> getLobbyMembers() {
        return new LinkedHashSet<>(lobbyMembers);
    }
    public void addLobbyMember(Player player) {
        if (lobbyMembers.add(player)) {
            Bukkit.getPluginManager().callEvent(new GamePlayerSessionJoin(player, this));
        }
    }
    public void removeLobbyMember(Player player) {
        if (lobbyMembers.remove(player)) {
            Bukkit.getPluginManager().callEvent(new GamePlayerSessionLeave(player, this));
        }
    }

    public SessionState getState() {
        return state;
    }
    public void setState(SessionState state) {
        this.state = state;
    }

    public MiniGame getMiniGame() {
        return miniGame;
    }
    public void setMiniGame(MiniGame miniGame) {
        this.miniGame = miniGame;
        if (miniGame != null && this.gameMap != null && !this.gameMap.supportsMiniGame(miniGame)) {
            setGameMap(null);
        }
    }

    public GameMap getGameMap() {
        return gameMap;
    }
    public void setGameMap(GameMap gameMap) {
        GameMapManager mapManager = GameMapManager.getInstance();
        if (gameMap != null) {
            // Slotted maps allow multiple sessions — slot system handles exclusivity at game start.
            if (!gameMap.hasArenaSlots()) {
                if (mapManager.isMapClaimed(gameMap.getId())) return;
                mapManager.addMapToClaimed(gameMap);
            }
        } else {
            if (this.gameMap != null && !this.gameMap.hasArenaSlots()) {
                mapManager.removeMapFromClaimed(this.gameMap);
            }
        }
        this.gameMap = gameMap;
        // Slotted maps: claimSlot() in startImmediately() sets arenaRegion at game start.
        if (gameMap == null) {
            this.arenaRegion = null;
        } else if (!gameMap.hasArenaSlots()) {
            this.arenaRegion = gameMap.getRegion();
        }
    }

    @Nullable
    public ArenaSlot getClaimedSlot() { return claimedSlot; }
    public void setClaimedSlot(@Nullable ArenaSlot claimedSlot) { this.claimedSlot = claimedSlot; }

    @Nullable
    public Region getArenaRegion() { return arenaRegion; }
    public void setArenaRegion(@Nullable Region arenaRegion) { this.arenaRegion = arenaRegion; }

    public int getTotalRounds() { return totalRounds; }
    public void setTotalRounds(int totalRounds) { this.totalRounds = Math.max(1, totalRounds); }

    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = Math.max(0, currentRound); }

    public boolean isAllowSpectate() { return allowSpectate; }
    public void setAllowSpectate(boolean allowSpectate) { this.allowSpectate = allowSpectate; }

    public boolean isShuffle() { return shuffle; }
    public void setShuffle(boolean shuffle) { this.shuffle = shuffle; }

    public SessionVisibility getVisibility() { return visibility; }
    public void setVisibility(SessionVisibility visibility) { this.visibility = visibility; }

    public List<Player> getWinners() { return Collections.unmodifiableList(winners); }
    public void setWinners(List<Player> w) { this.winners = new ArrayList<>(w); }
    public void clearWinners() { this.winners.clear(); }

    public Set<UUID> getInvitedPlayers() { return Collections.unmodifiableSet(invitedPlayers); }
    public void addInvitedPlayer(UUID uuid) { invitedPlayers.add(uuid); }
    public void removeInvitedPlayer(UUID uuid) { invitedPlayers.remove(uuid); }
    public boolean isInvited(UUID uuid) { return invitedPlayers.contains(uuid); }

    @Nullable public String getJoinCode() { return joinCode; }
    public void setJoinCode(@Nullable String joinCode) { this.joinCode = joinCode; }

    public Object getValue(String key, boolean isTemporary) {
        return getMap(isTemporary).get(key);
    }
    public Object[] getValues(boolean isTemporary) {
        return getMap(isTemporary).values().toArray();
    }
    public String[] getKeys(boolean isTemporary) {
        return getMap(isTemporary).keySet().toArray(new String[0]);
    }
    public void setValue(String key, Object o, boolean isTemporary) {
        getMap(isTemporary).put(key,o);
    }
    public void removeValue(String key, boolean isTemporary) {
        getMap(isTemporary).remove(key);
    }
    public void removeValues(boolean isTemporary) {
        getMap(isTemporary).clear();
    }
    public void removeValuesByPrefix(String prefix, boolean isTemporary) {
        getMap(isTemporary).keySet().removeIf(k -> k.startsWith(prefix));
    }

    private Map<String, Object> getMap(boolean isTemporary) {
        return isTemporary ? tempValues : values;
    }

    // ─── Role API ─────────────────────────────────────────────────────────────

    /** Returns the player's role in this session, or null if they are not a member. */
    @Nullable
    public SessionRole getRole(Player player) {
        if (lobbyMembers.contains(player)) return SessionRole.LOBBY;
        if (players.contains(player)) return SessionRole.PLAYER;
        if (spectators.contains(player)) return SessionRole.SPECTATOR;
        return null;
    }

    public MapSelectionMode getMapSelectionMode() { return mapSelectionMode; }
    public void setMapSelectionMode(MapSelectionMode mode) { this.mapSelectionMode = mode; }
    /** Derived compat — all existing call-sites unchanged. */
    public boolean isMapVoting() { return mapSelectionMode == MapSelectionMode.VOTE; }
    /** Compat wrapper: true → VOTE, false → SPECIFIC. Does not set RANDOM. */
    public void setMapVoting(boolean vote) { mapSelectionMode = vote ? MapSelectionMode.VOTE : MapSelectionMode.SPECIFIC; }
    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
    public boolean isEventSession() { return eventSession; }
    public void setEventSession(boolean eventSession) { this.eventSession = eventSession; }
    public @Nullable String getMapVote(Player player) { return mapVotes.get(player.getUniqueId()); }
    public void setMapVote(Player player, @Nullable String mapId) {
        if (mapId == null) mapVotes.remove(player.getUniqueId());
        else mapVotes.put(player.getUniqueId(), mapId);
    }
    public void clearMapVotes() { mapVotes.clear(); }
    public Map<UUID, String> getMapVotes() { return java.util.Collections.unmodifiableMap(mapVotes); }

    public @Nullable String getTeam(Player player) {
        return teamAssignments.get(player.getUniqueId());
    }
    public void setTeam(Player player, @Nullable String team) {
        if (team == null) teamAssignments.remove(player.getUniqueId());
        else teamAssignments.put(player.getUniqueId(), team);
    }
    public void clearTeams() {
        teamAssignments.clear();
    }

    public void setPendingAdminChange(String key, Object value) { pendingAdminChanges.put(key, value); }
    public Map<String, Object> drainPendingAdminChanges() {
        Map<String, Object> copy = new HashMap<>(pendingAdminChanges);
        pendingAdminChanges.clear();
        return copy;
    }
    public boolean hasPendingAdminChanges() { return !pendingAdminChanges.isEmpty(); }

    public void addBan(UUID uuid, String name) { bannedPlayers.put(uuid, name); }
    public boolean isBanned(UUID uuid) { return bannedPlayers.containsKey(uuid); }
    public void removeBan(UUID uuid) { bannedPlayers.remove(uuid); }
    /** Returns an unmodifiable view of banned UUID → name for tab-complete and display. */
    public java.util.Map<UUID, String> getBannedEntries() { return java.util.Collections.unmodifiableMap(bannedPlayers); }

    /**
     * Change a session member's role and fire {@link PlayerRoleChangeEvent}.
     * No-op if the player is not in this session or already has the target role.
     * MUST be called from the main thread (fires a Bukkit event).
     */
    public void setRole(Player player, SessionRole role) {
        SessionRole current = getRole(player);
        if (current == null) {
            SkGame.getInstance().getLogUtil().info(
                    "setRole called for player not in session: " + player.getName());
            return;
        }
        if (current == role) return;
        switch (current) {
            case LOBBY     -> lobbyMembers.remove(player);
            case PLAYER    -> players.remove(player);
            case SPECTATOR -> spectators.remove(player);
        }
        switch (role) {
            case LOBBY     -> lobbyMembers.add(player);
            case PLAYER    -> players.add(player);
            case SPECTATOR -> spectators.add(player);
        }
        Bukkit.getPluginManager().callEvent(new PlayerRoleChangeEvent(player, this, current, role));
    }

    /**
     * Returns a mutable snapshot of all session members (lobbyMembers + players + spectators).
     * Modifying the returned set does not affect session state.
     */
    public Set<Player> getMembers() {
        Set<Player> all = new HashSet<>(lobbyMembers);
        all.addAll(players);
        all.addAll(spectators);
        return all;
    }
}
