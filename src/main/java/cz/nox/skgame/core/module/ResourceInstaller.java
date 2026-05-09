package cz.nox.skgame.core.module;

import ch.njol.skript.Skript;
import ch.njol.skript.ScriptLoader;
import ch.njol.util.OpenCloseable;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ResourceInstaller {

    private final SkGame plugin;

    public ResourceInstaller(SkGame plugin) {
        this.plugin = plugin;
    }

    /**
     * Install always-on core paths then each enabled module's declared resource paths.
     * Script resources (paths starting with {@code "scripts/"}) are copied to
     * Skript's scripts folder under a {@code skgame/} subdirectory.
     * Files that already exist are skipped. Returns the count of newly installed files.
     */
    public int installAll(List<String> corePaths, Collection<SkGameModule> enabledModules) {
        List<String> allPaths = new ArrayList<>(corePaths);
        for (SkGameModule module : enabledModules) {
            allPaths.addAll(module.getResourcePaths());
        }

        // Deduplicate while preserving order; warn on collision.
        Set<String> unique = new LinkedHashSet<>();
        for (String path : allPaths) {
            if (!unique.add(path)) {
                plugin.getLogUtil().warning("Duplicate resource path declared by modules: " + path);
            }
        }

        File skgameDir = new File(Skript.getInstance().getScriptsFolder(), "skgame");
        skgameDir.mkdirs();

        Set<File> newlyCreated = new HashSet<>();
        int count = 0;

        for (String resourcePath : unique) {
            String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            File dest = new File(skgameDir, fileName);
            if (dest.exists()) continue;

            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in == null) {
                    plugin.getLogUtil().warning("Bundled resource " + resourcePath + " missing from jar");
                    continue;
                }
                Files.copy(in, dest.toPath());
                newlyCreated.add(dest);
                count++;
                plugin.getLogUtil().info("Installed resource: " + fileName);
            } catch (IOException e) {
                plugin.getLogUtil().error("Failed to install " + resourcePath + ": " + e.getMessage());
            }
        }

        if (!newlyCreated.isEmpty()) {
            ScriptLoader.loadScripts(newlyCreated, OpenCloseable.EMPTY);
        }

        return count;
    }
}
