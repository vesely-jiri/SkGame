package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;
import cz.nox.skgame.core.gui.services.LeaderboardGuiService;
import cz.nox.skgame.core.storage.DatabaseManager;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.List;

/** Phase 7: SQLite backend, statistics API, leaderboards GUI. */
public class DatabaseModule implements SkGameModule {

    @Override public String getId() { return "database"; }
    @Override public boolean isEnabledByDefault() { return false; }
    @Override public boolean canEnable(SkGame plugin) { return true; }

    @Override
    public List<String> getSkriptClasses() {
        return List.of(
                "cz.nox.skgame.skript.expressions.statistics.ExprPlayerGameResults",
                "cz.nox.skgame.skript.expressions.statistics.ExprPlayerWinCount",
                "cz.nox.skgame.skript.expressions.statistics.ExprPlayerPlayCount",
                "cz.nox.skgame.skript.expressions.statistics.ExprTopPlayers",
                "cz.nox.skgame.skript.expressions.statistics.ExprGameResultField"
        );
    }

    @Override
    public void onEnable(SkGame plugin) {
        try {
            DatabaseManager.getInstance().initialize(plugin);
        } catch (SQLException e) {
            plugin.getLogUtil().warning("DatabaseModule: failed to initialize — " + e.getMessage()
                    + ". Game results will not be persisted.");
        }
        Bukkit.getPluginManager().registerEvents(LeaderboardGuiService.getInstance(), plugin);
    }

    @Override
    public void onDisable(SkGame plugin) {
        DatabaseManager.getInstance().shutdown();
    }
}
