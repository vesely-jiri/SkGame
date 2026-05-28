package cz.nox.skgame.util;

import cz.nox.skgame.SkGame;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

/**
 * Merges missing keys from the bundled config.yml default into the installed config.yml.
 * Preserves user-set values and YAML comments via boosted-yaml's comment-aware parser.
 * Run once on startup, after saveDefaultConfig() and migrateConfig(), before reloadConfig().
 */
public class ConfigAutoMerge {

    public static void run(SkGame plugin) {
        // Diff first: keys in bundled default absent from installed config.
        Set<String> installedKeys = plugin.getConfig().getKeys(true);
        Set<String> defaultKeys;
        try (InputStreamReader reader = new InputStreamReader(
                plugin.getResource("config.yml"), StandardCharsets.UTF_8)) {
            defaultKeys = YamlConfiguration.loadConfiguration(reader).getKeys(true);
        } catch (Exception e) {
            plugin.getLogger().warning("Config auto-merge: failed to read bundled defaults — " + e.getMessage());
            return;
        }

        Set<String> missing = new TreeSet<>(defaultKeys);
        missing.removeAll(installedKeys);
        if (missing.isEmpty()) return;

        // Merge via boosted-yaml: adds missing keys with their template comments,
        // preserves all existing user values and formatting.
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            YamlDocument document = YamlDocument.create(
                    configFile,
                    plugin.getResource("config.yml"),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setKeepAll(true).build()
            );
            document.save();
        } catch (Exception e) {
            plugin.getLogger().warning("Config auto-merge: failed to save merged config — " + e.getMessage());
            return;
        }

        for (String key : missing) {
            plugin.getLogger().info("Config auto-merge: added '" + key + "'");
        }
    }

    private ConfigAutoMerge() {}
}
