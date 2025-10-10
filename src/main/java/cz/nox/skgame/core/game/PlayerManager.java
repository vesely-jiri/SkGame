package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.model.GamePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private static PlayerManager playerManager;
    private final Map<UUID, GamePlayer> players = new HashMap<>();

    public static synchronized PlayerManager getInstance() {
        if (playerManager == null) {
            playerManager = new PlayerManager();
        }
        return playerManager;
    }

    public GamePlayer createPlayer(Player player) {
        GamePlayer gamePlayer = new GamePlayer(player);
        players.put(player.getUniqueId(),gamePlayer);
        return gamePlayer;
    }

    public void deletePlayer(Player player) {
        players.remove(player.getUniqueId());
    }

    public GamePlayer getPlayer(Player player) {
        return players.computeIfAbsent(player.getUniqueId(), id -> new GamePlayer(player));
    }
}
