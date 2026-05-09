package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

public class WorldGuardRegionModule implements SkGameModule {

    @Override public String getId() { return "region-worldguard"; }

    @Override
    public boolean canEnable(SkGame plugin) {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[SkGame] Module region-worldguard disabled: WorldGuard plugin not found");
            return false;
        }
    }

    @Override public void onEnable(SkGame plugin) {}
}
