package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;
import cz.nox.skgame.core.storage.DatabaseManager;

import java.sql.SQLException;

/** Phase 7: SQLite/MySQL backend, statistics API. */
public class DatabaseModule implements SkGameModule {

    @Override public String getId() { return "database"; }
    @Override public boolean isEnabledByDefault() { return false; }
    @Override public boolean canEnable(SkGame plugin) { return true; }

    @Override
    public void onEnable(SkGame plugin) {
        try {
            DatabaseManager.getInstance().initialize(plugin);
        } catch (SQLException e) {
            plugin.getLogUtil().warning("DatabaseModule: failed to initialize — " + e.getMessage()
                    + ". Game results will not be persisted.");
        }
    }

    @Override
    public void onDisable(SkGame plugin) {
        DatabaseManager.getInstance().shutdown();
    }
}
