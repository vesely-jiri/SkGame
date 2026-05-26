package cz.nox.skgame.util;

import java.io.InputStream;
import java.util.Properties;

public final class BuildInfo {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = BuildInfo.class.getResourceAsStream("/build-info.properties")) {
            if (is != null) PROPS.load(is);
        } catch (Exception ignored) {}
    }

    public static String version() {
        return PROPS.getProperty("version", "unknown");
    }

    public static String gitSha() {
        return PROPS.getProperty("git.sha", "unknown");
    }

    private BuildInfo() {}
}
