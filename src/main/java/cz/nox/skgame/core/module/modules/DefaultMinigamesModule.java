package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

import java.util.Set;

public class DefaultMinigamesModule implements SkGameModule {

    @Override public String getId() { return "default-minigames"; }
    @Override public Set<String> getDependencies() { return Set.of("gui"); }
    @Override public boolean canEnable(SkGame plugin) { return true; }
    @Override public void onEnable(SkGame plugin) {}
}
