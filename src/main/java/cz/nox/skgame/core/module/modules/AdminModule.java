package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

import java.util.List;

public class AdminModule implements SkGameModule {

    @Override public String getId() { return "admin"; }
    @Override public boolean canEnable(SkGame plugin) { return true; }
    @Override public void onEnable(SkGame plugin) {}

    @Override
    public List<String> getResourcePaths() {
        return List.of();
    }
}
