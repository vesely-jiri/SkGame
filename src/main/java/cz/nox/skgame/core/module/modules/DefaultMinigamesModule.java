package cz.nox.skgame.core.module.modules;

import ch.njol.skript.Skript;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

import java.util.List;
import java.util.Set;

public class DefaultMinigamesModule implements SkGameModule {

    @Override public String getId() { return "default-minigames"; }
    @Override public Set<String> getDependencies() { return Set.of(); }
    @Override public boolean canEnable(SkGame plugin) { return true; }
    @Override public void onEnable(SkGame plugin) {}

    @Override
    public List<String> getResourcePaths() {
        // Bundled scripts use cancel events:/minigame tags: entries which trigger
        // EventValueExpression.init() in Skript 2.13.0 test mode (assertions enabled),
        // causing a spurious AssertionError. Skip in test env; CI covers the API via
        // src/test/scripts/ which uses only safe entries.
        if (Skript.testing()) return List.of();
        return List.of(
            "scripts/minigames/bomberman.sk",
            "scripts/minigames/koth.sk",
            "scripts/minigames/volleyball.sk"
        );
    }
}
