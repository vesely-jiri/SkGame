package cz.nox.skgame.api.game.model;

import cz.nox.skgame.api.game.event.GamePlayerSessionJoin;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class Session {
    private String id;
    private Player host;
    private HashSet<Player> players;
    private HashSet<Player> spectators;
    private SessionState state;
    private MiniGame miniGame;
    private GameMap gameMap;
    private Map<String, Object> values;
    private Map<String, Object> tempValues;

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
        this(id,null,null,SessionState.STOPPED,null,
                new HashSet<>(),new HashSet<>(),new HashMap<>(),new HashMap<>());
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
    }

    public GameMap getGameMap() {
        return gameMap;
    }
    public void setGameMap(GameMap gameMap) {
        GameMapManager mapManager = GameMapManager.getInstance();
        if (gameMap != null) {
            mapManager.addMapToClaimed(gameMap);
        } else {
            mapManager.removeMapFromClaimed(this.getGameMap());
        }
        this.gameMap = gameMap;
    }

    public Object getValue(String key, boolean isTemporary) {
        return getMap(isTemporary).get(key);
    }
    public Collection<Object> getValues(boolean isTemporary) {
        return getMap(isTemporary).values();
    }
    public Collection<String> getKeys(boolean isTemporary) {
        return getMap(isTemporary).keySet();
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
}
