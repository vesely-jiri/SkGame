package cz.nox.skgame.util;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

public class LogUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private static final String PREFIX_INFO  = "&8[&3SkGame&8] &7";
    private static final String PREFIX_WARN  = "&8[&3SkGame&8] &e";
    private static final String PREFIX_ERROR = "&8[&3SkGame&8] &c";

    public void info(String message) {
        send(PREFIX_INFO + message);
    }

    public void warning(String message) {
        send(PREFIX_WARN + message);
    }

    public void error(String message) {
        send(PREFIX_ERROR + message);
    }

    public void raw(String message) {
        send(message);
    }

    private void send(String message) {
        Bukkit.getConsoleSender().sendMessage(LEGACY.deserialize(message));
    }
}
