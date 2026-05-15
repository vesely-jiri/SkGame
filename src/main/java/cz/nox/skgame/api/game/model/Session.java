package cz.nox.skgame.api.game.model;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.GamePlayerSessionJoin;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.PlayerRoleChangeEvent;
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
    }

    @Nullable
    public ArenaSlot getClaimedSlot() { return claimedSlot; }
    public void setClaimedSlot(@Nullable ArenaSlot claimedSlot) { this.claimedSlot = claimedSlot; }

    @Nullable
    public Region getArenaRegion() { return arenaRegion; }
    public void setArenaRegion(@Nullable Region arenaRegion) { this.arenaRegion = arenaRegion; }

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
     * Returns a mutable snapshot of all session members (lobbyMembers ∪ players ∪ spectators).
     * Modifying the returned set does not affect session state.
     */
    public Set<Player> getMembers() {
        Set<Player> all = new HashSet<>(lobbyMembers);
        all.addAll(players);
        all.addAll(spectators);
        return all;
    }
}
