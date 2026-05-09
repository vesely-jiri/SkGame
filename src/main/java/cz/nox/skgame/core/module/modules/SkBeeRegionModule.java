package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;
import cz.nox.skgame.core.region.RegionFactory;
import cz.nox.skgame.core.region.SkBeeBoundRegion;

import java.util.List;

public class SkBeeRegionModule implements SkGameModule {

    @Override public String getId() { return "region-skbee"; }

    @Override
    public boolean canEnable(SkGame plugin) {
        try {
            Class.forName("com.shanebeestudios.skbee.SkBee");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogUtil().info("Module region-skbee disabled: SkBee plugin not found");
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEnable(SkGame plugin) {
        if (SkBeeBoundRegion.init()) {
            Class<Object> boundClass = (Class<Object>) SkBeeBoundRegion.getBoundClass();
            RegionFactory.register(boundClass, SkBeeBoundRegion::new);
            plugin.getLogUtil().info("SkBee region adapter registered");
        } else {
            plugin.getLogUtil().warning("SkBee found but region adapter init failed");
        }
    }

    @Override
    public List<String> getSkriptClasses() {
        return List.of("cz.nox.skgame.skript.expressions.regions.ExprRegionBoundCorner");
    }
}
