package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;
import cz.nox.skgame.core.scoreboard.ScoreboardService;
import org.bukkit.Bukkit;

import java.util.List;

public class ScoreboardModule implements SkGameModule {

    @Override public String getId() { return "scoreboard"; }
    @Override public boolean isEnabledByDefault() { return true; }
    @Override public boolean canEnable(SkGame plugin) { return true; }

    @Override
    public void onEnable(SkGame plugin) {
        ScoreboardService.getInstance().init(plugin);
        Bukkit.getPluginManager().registerEvents(ScoreboardService.getInstance(), plugin);
    }

    @Override
    public List<String> getSkriptClasses() { return List.of(); }
}
