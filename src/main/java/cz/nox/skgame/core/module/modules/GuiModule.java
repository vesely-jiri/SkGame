package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

import java.util.List;

public class GuiModule implements SkGameModule {

    @Override public String getId() { return "gui"; }
    @Override public boolean canEnable(SkGame plugin) { return true; }
    @Override public void onEnable(SkGame plugin) {}

    @Override
    public List<String> getResourcePaths() {
        return List.of();
    }
}
