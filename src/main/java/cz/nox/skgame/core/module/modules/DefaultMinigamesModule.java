package cz.nox.skgame.core.module.modules;

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
        return List.of(
            "scripts/minigames/bomberman.sk",
            "scripts/minigames/koth.sk",
            "scripts/minigames/volleyball.sk"
        );
    }
}
