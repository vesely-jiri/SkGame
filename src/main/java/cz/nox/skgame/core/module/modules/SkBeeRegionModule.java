package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

public class SkBeeRegionModule implements SkGameModule {

    @Override public String getId() { return "region-skbee"; }

    @Override
    public boolean canEnable(SkGame plugin) {
        try {
            Class.forName("com.shanebeestudios.skbee.SkBee");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[SkGame] Module region-skbee disabled: SkBee plugin not found");
            return false;
        }
    }

    @Override public void onEnable(SkGame plugin) {}
}
