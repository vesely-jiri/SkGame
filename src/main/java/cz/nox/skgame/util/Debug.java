package cz.nox.skgame.util;

import cz.nox.skgame.SkGame;

import java.util.function.Supplier;

public final class Debug {

    private Debug() {}

    public static void log(String category, String message) {
        if (!SkGame.getInstance().getConfig().getBoolean("debug", false)) return;
        SkGame.getInstance().getLogger().info("[DEBUG][" + category + "] " + message);
    }

    public static void log(String category, Supplier<String> message) {
        if (!SkGame.getInstance().getConfig().getBoolean("debug", false)) return;
        SkGame.getInstance().getLogger().info("[DEBUG][" + category + "] " + message.get());
    }
}
