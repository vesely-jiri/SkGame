package cz.nox.skgame.api.game.model;

import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class Session {
    private String id;
    private String name;
    private Player host;
    private HashSet<Player> players;
    private HashSet<Player> spectators;
    private SessionState state;
    private MiniGame miniGame;
    private GameMap gameMap;
    private HashMap<String, Object> values;

    public Session(String id, String name, Player host, MiniGame miniGame,
                   SessionState state, GameMap map, HashSet<Player> players, HashSet<Player> spectators,
                   HashMap<String, Object> values) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.players = new HashSet<>(players);
        this.spectators = new HashSet<>(spectators);
        this.state = state;
        this.miniGame = miniGame;
        this.gameMap = map;
        this.values = new HashMap<>(values);
    }

    public Session(String id) {
        this(id,null,null,null,SessionState.STOPPED,null,
                new HashSet<>(),new HashSet<>(),new HashMap<>());
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
        this.players.addAll(Arrays.asList(players));
    }
    public void removePlayers(Player... players) {
        for (Player player : players) {
            this.players.remove(player);
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

    public Object getValue(String key) {
        return values.get(key);
    }
    public Collection<Object> getValues() {
        return values.values();
    }
    public Collection<String> getKeys() {
        return values.keySet();
    }
    public void setValue(String key, Object o) {
        if (o == null) {
            values.remove(key);
        } else {
            values.put(key, o);
        }
    }
}
