package cz.nox.skgame.api.game.model;

import cz.nox.skgame.api.game.model.type.SessionState;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class Session implements SessionReadOnly {
    private String id;
    private String name;
    private Player host;
    private GameMode gameMode;
    private SessionState state;
    private GameMap gameMap;
    private HashSet<Player> players;
    private HashSet<Player> spectators;

    public Session(String id, String name, Player host, GameMode gameMode,
                   SessionState state, GameMap map, HashSet<Player> players, HashSet<Player> spectators) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.gameMode = gameMode;
        this.state = state;
        this.gameMap = map;
        this.players = new HashSet<>(players);
        this.spectators = new HashSet<>(spectators);
    }

    public Session(String id) {
        this(id,null,null,null,SessionState.STOPPED,null,
                new HashSet<>(),new HashSet<>());
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
    public GameMode getGameMode() {
        return gameMode;
    }
    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }
    public SessionState getState() {
        return state;
    }
    public void setState(SessionState gameState) {
        this.state = gameState;
    }
    public GameMap getGameMap() {
        return gameMap;
    }
    public void setGameMap(GameMap map) {
        this.gameMap = map;
    }
    public HashSet<Player> getPlayers() {
        return players;
    }
    public void setPlayers(HashSet<Player> players) {
        this.players = players;
    }
    public HashSet<Player> getSpectators() {
        return spectators;
    }
    public void setSpectators(HashSet<Player> spectators) {
        this.spectators = spectators;
    }
}