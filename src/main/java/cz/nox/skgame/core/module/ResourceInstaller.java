package cz.nox.skgame.core.module;

import ch.njol.skript.Skript;
import ch.njol.skript.ScriptLoader;
import ch.njol.util.OpenCloseable;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.ResourceTarget;
import cz.nox.skgame.api.module.SkGameModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class ResourceInstaller {

    private final SkGame plugin;

    public ResourceInstaller(SkGame plugin) {
        this.plugin = plugin;
    }

    /**
     * Install always-on core paths then each enabled module's declared resource paths.
     * Destination for each file is determined by the module's {@link SkGameModule#resolveResourceTarget}
     * (or the built-in scripts-folder logic for core paths). Files that already exist are skipped.
     * Returns the count of newly installed files.
     */
    public int installAll(List<String> corePaths, Collection<SkGameModule> enabledModules) {
        // Build ordered work list: path → target (deduplicated, first-wins).
        LinkedHashMap<String, ResourceTarget> workList = new LinkedHashMap<>();

        for (String path : corePaths) {
            try {
                ResourceTarget target = resolveScriptsTarget(path);
                if (workList.put(path, target) != null) {
                    plugin.getLogUtil().warning("Duplicate core resource path: " + path);
                }
            } catch (IllegalStateException e) {
                plugin.getLogUtil().error("Invalid core resource path '" + path + "': " + e.getMessage());
            }
        }

        for (SkGameModule module : enabledModules) {
            for (String path : module.getResourcePaths()) {
                try {
                    ResourceTarget target = module.resolveResourceTarget(path, plugin);
                    if (workList.put(path, target) != null) {
                        plugin.getLogUtil().warning("Duplicate resource path declared by modules: " + path);
                    }
                } catch (IllegalStateException e) {
                    plugin.getLogUtil().error("Module '" + module.getId()
                            + "' has invalid resource path '" + path + "': " + e.getMessage());
                }
            }
        }

        Set<File> newScripts = new HashSet<>();
        int count = 0;

        for (var entry : workList.entrySet()) {
            String resourcePath = entry.getKey();
            ResourceTarget target = entry.getValue();

            target.directory().mkdirs();
            File dest = new File(target.directory(), target.relativePath());
            if (dest.exists()) continue;

            File parent = dest.getParentFile();
            if (!parent.exists() && !parent.mkdirs()) {
                plugin.getLogUtil().warning(
                        "Failed to create directory " + parent + " — skipping " + resourcePath);
                continue;
            }

            try (InputStream in = plugin.getResource(resourcePath)) {
                if (in == null) {
                    plugin.getLogUtil().warning("Bundled resource " + resourcePath + " missing from jar");
                    continue;
                }
                Files.copy(in, dest.toPath());
                if (dest.getName().endsWith(".sk")) newScripts.add(dest);
                count++;
                plugin.getLogUtil().info("Installed resource: " + target.relativePath());
            } catch (IOException e) {
                plugin.getLogUtil().error("Failed to install " + resourcePath + ": " + e.getMessage());
            }
        }

        if (!newScripts.isEmpty()) {
            ScriptLoader.loadScripts(newScripts, OpenCloseable.EMPTY);
        }

        return count;
    }

    /** Handles the built-in "scripts/" prefix for always-on core resources. */
    private static ResourceTarget resolveScriptsTarget(String resourcePath) {
        if (resourcePath.startsWith("scripts/")) {
            return new ResourceTarget(
                    new File(Skript.getInstance().getScriptsFolder(), "skgame"),
                    resourcePath.substring("scripts/".length())
            );
        }
        throw new IllegalStateException("Core resource path must start with 'scripts/': " + resourcePath);
    }
}
