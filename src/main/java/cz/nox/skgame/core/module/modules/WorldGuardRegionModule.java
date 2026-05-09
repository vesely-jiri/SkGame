package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;
import cz.nox.skgame.core.region.WorldGuardRegion;

import java.util.List;

public class WorldGuardRegionModule implements SkGameModule {

    @Override public String getId() { return "region-worldguard"; }

    @Override
    public boolean canEnable(SkGame plugin) {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogUtil().info("Module region-worldguard disabled: WorldGuard plugin not found");
            return false;
        }
    }

    @Override
    public void onEnable(SkGame plugin) {
        if (WorldGuardRegion.init()) {
            plugin.getLogUtil().info("WorldGuard region adapter registered");
        } else {
            plugin.getLogUtil().warning("WorldGuard found but region adapter init failed");
        }
    }

    @Override
    public List<String> getSkriptClasses() {
        return List.of("cz.nox.skgame.skript.expressions.regions.ExprWorldGuardRegion");
    }
}
