package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.api.module.ResourceTarget;
import cz.nox.skgame.api.module.SkGameModule;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesModule implements SkGameModule {

    @Override public String getId() { return "messages"; }
    @Override public boolean canEnable(SkGame plugin) { return true; }

    @Override
    public void onEnable(SkGame plugin) {
        File messagesDir = new File(plugin.getDataFolder(), "messages");
        messagesDir.mkdirs();
        Messages.load(messagesDir, plugin.getConfig(), plugin.getLogger());

        if (plugin.getConfig().getBoolean("messages.auto-merge", true)) {
            Map<String, YamlConfiguration> bundled = new HashMap<>();
            for (String locale : List.of("en_US", "cs_CZ")) {
                InputStream stream = plugin.getResource("messages/messages_" + locale + ".yml");
                if (stream != null) {
                    bundled.put(locale, YamlConfiguration.loadConfiguration(
                            new InputStreamReader(stream, StandardCharsets.UTF_8)));
                }
            }
            if (!bundled.isEmpty()) {
                Messages.autoMerge(messagesDir, bundled, plugin.getLogger());
            }
        }

        int loaded = Messages.getLoadedLocales().size();
        if (loaded == 0) {
            plugin.getLogUtil().warning("MessagesModule: no locale files found in "
                    + messagesDir.getPath() + " — install messages_en_US.yml to enable localization");
        } else {
            plugin.getLogUtil().info("MessagesModule: loaded " + loaded
                    + " locale(s): " + Messages.getLoadedLocales());
        }
    }

    @Override
    public void onDisable(SkGame plugin) {
        Messages.clear();
    }

    @Override
    public List<String> getSkriptClasses() {
        return List.of(
                "cz.nox.skgame.skript.messages.EffSendMessage",
                "cz.nox.skgame.skript.messages.ExprMessage"
        );
    }

    @Override
    public List<String> getResourcePaths() {
        return List.of(
                "messages/messages_en_US.yml",
                "messages/messages_cs_CZ.yml"
        );
    }

    @Override
    public ResourceTarget resolveResourceTarget(String resourcePath, SkGame plugin) {
        if (resourcePath.startsWith("messages/")) {
            return new ResourceTarget(
                    new File(plugin.getDataFolder(), "messages"),
                    resourcePath.substring("messages/".length())
            );
        }
        return SkGameModule.super.resolveResourceTarget(resourcePath, plugin);
    }
}
