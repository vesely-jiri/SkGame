package cz.nox.skgame.util;

import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public class LogUtil {
    private final Logger logger;
    private static final String PREFIX = "[SkGame] ";

    public LogUtil(Plugin instance) {
        this.logger = instance.getLogger();
    }

    public void info(String message) {
        logger.info(PREFIX + message);
    }

    public void warning(String message) {
        logger.warning(PREFIX + message);
    }

    public void error(String message) {
        logger.severe(PREFIX + message);
    }
}
