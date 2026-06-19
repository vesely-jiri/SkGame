package cz.nox.skgame.core.locale;

import cz.nox.skgame.api.messages.Messages;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for script-defined locale entries registered via the {@code locale "ns":} structure.
 * Stores: namespace → key → localeCode → text.
 */
public final class ScriptLocaleRegistry {

    private static final ScriptLocaleRegistry INSTANCE = new ScriptLocaleRegistry();

    // namespace → key → localeCode → text
    private final Map<String, Map<String, Map<String, String>>> data = new ConcurrentHashMap<>();

    private ScriptLocaleRegistry() {}

    public static ScriptLocaleRegistry getInstance() {
        return INSTANCE;
    }

    public void register(String namespace, Map<String, Map<String, String>> entries) {
        data.put(namespace, new ConcurrentHashMap<>(entries));
    }

    public void unregister(String namespace) {
        data.remove(namespace);
    }

    /**
     * Look up a translated string for the given player.
     * Fallback chain: forced locale → player locale → language-only → en_US → en.
     * Returns null if namespace/key not found or no matching locale.
     */
    public @Nullable String get(String namespace, String key, @Nullable Player player) {
        Map<String, Map<String, String>> nsMap = data.get(namespace);
        if (nsMap == null) return null;
        Map<String, String> keyMap = nsMap.get(key);
        if (keyMap == null) return null;

        String locale;
        String forced = Messages.getForcedLocale();
        if (forced != null && !forced.isEmpty()) {
            locale = forced;
        } else if (player != null) {
            locale = Messages.normalize(player.getLocale());
        } else {
            locale = "en_US";
        }

        String text = keyMap.get(locale);
        if (text != null) return text;

        // Language-only fallback (e.g. "cs" when locale is "cs_CZ")
        int idx = locale.indexOf('_');
        if (idx > 0) {
            text = keyMap.get(locale.substring(0, idx));
            if (text != null) return text;
        }

        text = keyMap.get("en_US");
        if (text != null) return text;
        return keyMap.get("en");
    }
}
