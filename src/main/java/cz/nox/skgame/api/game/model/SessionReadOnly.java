package cz.nox.skgame.api.game.model;

import cz.nox.skgame.api.game.model.type.SessionState;
import org.bukkit.entity.Player;

import java.util.HashSet;

public interface SessionReadOnly {
    String getId();
    String getName();
    Player getHost();
    GameMode getGameMode();
    GameMap getGameMap();
    SessionState getState();
    HashSet<Player> getPlayers();
    HashSet<Player> getSpectators();
}
