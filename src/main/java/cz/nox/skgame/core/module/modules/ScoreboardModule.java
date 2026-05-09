package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

/** Phase 10: scoreboard helpers. Phase 5: empty shell. */
public class ScoreboardModule implements SkGameModule {

    @Override public String getId() { return "scoreboard"; }
    @Override public boolean canEnable(SkGame plugin) { return true; }
    @Override public void onEnable(SkGame plugin) {}
}
