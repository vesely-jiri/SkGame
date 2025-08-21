package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.model.GameMode;

import java.util.Map;

public class GameModeManager {

    private static GameModeManager gameModeManager;
    private Map<String, GameMode> gameModes;
    private GameMode lastCreatedGameMode;

    public static GameModeManager getInstance() {
        if (gameModeManager == null) gameModeManager = new GameModeManager();
        return gameModeManager;
    }

    public void createGameMode(String id) {
        if (gameModes.containsKey(id)) return;
        GameMode gameMode = new GameMode(id);
        gameModes.put(id,gameMode);
        lastCreatedGameMode = gameMode;
    }
}
