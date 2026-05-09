package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

/** Phase 7: SQLite/MySQL backend, statistics API. Phase 5: empty shell. */
public class DatabaseModule implements SkGameModule {

    @Override public String getId() { return "database"; }
    @Override public boolean isEnabledByDefault() { return false; }
    @Override public boolean canEnable(SkGame plugin) { return true; }
    @Override public void onEnable(SkGame plugin) {}
}
