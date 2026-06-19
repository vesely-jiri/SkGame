package cz.nox.skgame.api.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Static message service. Initialized by {@code MessagesModule.onEnable()}.
 * All methods are thread-safe. Static state is reset by {@link #clear()}.
 */
public final class Messages {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)\\}");

    private static final Map<String, YamlConfiguration> locales = new ConcurrentHashMap<>();
    private static final Set<String> warnedKeys = ConcurrentHashMap.newKeySet();

    /** Sentinel: key exists but value is "". Means "suppress this message — don't send anything." */
    private static final Resolved SUPPRESS = new Resolved(Format.LEGACY, "");

    @Nullable private static String forcedLocale = null;
    private static String fallbackLocale = "en_US";
    @Nullable private static Logger logger = null;

    private Messages() {}

    // ─── Init / teardown ─────────────────────────────────────────────────────

    /**
     * Load all {@code messages_<locale>.yml} files from {@code messagesDir}.
     * Reads {@code messages.forced-locale} and {@code messages.fallback-locale} from config.
     * Safe to call multiple times (hot reload); clears previous state first.
     */
    public static void load(File messagesDir, FileConfiguration config, Logger log) {
        logger = log;
        locales.clear();
        warnedKeys.clear();

        forcedLocale = config.getString("messages.forced-locale");
        fallbackLocale = config.getString("messages.fallback-locale", "en_US");

        if (!messagesDir.exists() || !messagesDir.isDirectory()) return;

        File[] files = messagesDir.listFiles((dir, name) ->
                name.startsWith("messages_") && name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();
            String locale = name.substring("messages_".length(), name.length() - ".yml".length());
            locales.put(locale, YamlConfiguration.loadConfiguration(file));
        }

        if (!locales.containsKey("en_US")) {
            warn("messages_en_US.yml missing — message lookups will return missing placeholders");
        }
    }

    /** Clear all loaded locales and the warned-key dedup set. Call on module disable. */
    public static void clear() {
        locales.clear();
        warnedKeys.clear();
    }

    /** Returns the set of currently loaded locale identifiers (e.g. "en_US", "cs_CZ"). */
    public static Set<String> getLoadedLocales() {
        return locales.keySet();
    }

    // ─── Auto-merge ───────────────────────────────────────────────────────────

    /**
     * For each locale in {@code bundledDefaults}, appends any keys present in the bundled
     * YAML but absent in the user's on-disk file. Saves to disk and refreshes in-memory map.
     * No-op when the user file is missing or already complete.
     */
    public static void autoMerge(File messagesDir, Map<String, YamlConfiguration> bundledDefaults, Logger log) {
        for (Map.Entry<String, YamlConfiguration> entry : bundledDefaults.entrySet()) {
            String locale = entry.getKey();
            YamlConfiguration bundled = entry.getValue();
            File userFile = new File(messagesDir, "messages_" + locale + ".yml");
            if (!userFile.exists()) continue;

            YamlConfiguration userConfig = locales.getOrDefault(locale,
                    YamlConfiguration.loadConfiguration(userFile));

            Set<String> missing = new TreeSet<>(getLeafKeys(bundled));
            missing.removeAll(getLeafKeys(userConfig));
            if (missing.isEmpty()) continue;

            for (String key : missing) {
                userConfig.set(key, bundled.get(key));
                if (log != null) log.info("[Messages] Auto-merged key '" + key + "' into messages_" + locale + ".yml");
            }
            try {
                userConfig.save(userFile);
                locales.put(locale, userConfig);
                if (log != null) log.info("[Messages] Auto-merged " + missing.size()
                        + " missing key(s) for locale " + locale);
            } catch (IOException e) {
                if (log != null) log.warning("[SkGame/Messages] Auto-merge: could not save messages_"
                        + locale + ".yml — " + e.getMessage());
            }
        }
    }

    private static Set<String> getLeafKeys(YamlConfiguration config) {
        return config.getKeys(true).stream()
                .filter(k -> !config.isConfigurationSection(k))
                .collect(Collectors.toSet());
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Look up a message for a player's locale with positional args.
     * Returns a String with legacy § color codes for legacy-format messages,
     * or a legacy-serialized string for MiniMessage-format messages.
     * Null args are stringified to "null" via String.valueOf.
     */
    public static String get(String key, @Nullable Player player, Object... args) {
        return getForLocale(key, resolveLocale(player), args);
    }

    /**
     * Look up a message for a specific locale string.
     * Falls back to {@code fallbackLocale} if the key is absent in the requested locale.
     * Null args are stringified to "null" via String.valueOf.
     */
    public static String getForLocale(String key, String locale, Object... args) {
        Resolved resolved = lookup(key, locale);
        if (resolved == SUPPRESS) return "";
        if (resolved == null) resolved = lookup(key, fallbackLocale);
        if (resolved == SUPPRESS) return "";
        if (resolved == null) return missing(key);
        return applyAndFormatString(resolved, args);
    }

    /**
     * Same as {@link #get} but returns an Adventure Component.
     * Preferred over {@link #get} when sending via Paper's Component API.
     * Null args are stringified to "null" via String.valueOf.
     */
    public static Component getComponent(String key, @Nullable Player player, Object... args) {
        Component c = resolveComponent(key, resolveLocale(player), args);
        if (c != null) return c;
        c = resolveComponent(key, fallbackLocale, args);
        if (c != null) return c;
        return Component.text(missing(key));
    }

    /** Send a localized message to a player using their own locale. */
    public static void send(Player player, String key, Object... args) {
        Component c = resolveComponent(key, resolveLocale(player), args);
        if (c == null) c = resolveComponent(key, fallbackLocale, args);
        if (c == null) { missing(key); return; } // warn once, skip send
        player.sendMessage(c);
    }

    /**
     * Send to any CommandSender.
     * Players receive their own locale; non-players (console) use the fallback locale.
     */
    public static void send(CommandSender target, String key, Object... args) {
        if (target instanceof Player player) {
            send(player, key, args);
        } else {
            String msg = getForLocale(key, fallbackLocale, args);
            if (!msg.isEmpty()) target.sendMessage(msg);
        }
    }

    /** Returns the formatted component, or null if key is suppressed (empty value). */
    private static @Nullable Component resolveComponent(String key, String locale, Object[] args) {
        Resolved resolved = lookup(key, locale);
        if (resolved == null || resolved == SUPPRESS) return null;
        return applyAndFormatComponent(resolved, args);
    }

    // ─── Locale resolution ───────────────────────────────────────────────────

    /** Returns the currently configured forced locale, or null if not set. */
    public static @Nullable String getForcedLocale() {
        return forcedLocale;
    }

    public static String resolveLocale(@Nullable Player player) {
        if (forcedLocale != null && locales.containsKey(forcedLocale)) return forcedLocale;
        if (player == null) return fallbackLocale;
        String normalized = normalize(player.getLocale());
        return locales.containsKey(normalized) ? normalized : fallbackLocale;
    }

    /**
     * Normalize Bukkit's lowercase locale string to ISO convention.
     * Examples: "cs_cz" → "cs_CZ", "en_us" → "en_US", "de" → "de".
     */
    public static String normalize(String locale) {
        if (locale == null || locale.isEmpty()) return "en_US";
        int idx = locale.indexOf('_');
        if (idx < 0) return locale.toLowerCase(Locale.ROOT);
        String lang    = locale.substring(0, idx).toLowerCase(Locale.ROOT);
        String country = locale.substring(idx + 1).toUpperCase(Locale.ROOT);
        return lang + "_" + country;
    }

    // ─── Internal lookup + formatting ────────────────────────────────────────

    private static @Nullable Resolved lookup(String key, String locale) {
        YamlConfiguration yaml = locales.get(locale);
        if (yaml == null) return null;

        String rawString = yaml.getString(key);
        if (rawString != null) return rawString.isEmpty() ? SUPPRESS : new Resolved(Format.LEGACY, rawString);

        String format = yaml.getString(key + ".format");
        String text   = yaml.getString(key + ".text");
        if ("minimessage".equals(format) && text != null)
            return new Resolved(Format.MINIMESSAGE, text);

        return null;
    }

    @SuppressWarnings("deprecation")
    private static String applyAndFormatString(Resolved resolved, Object[] args) {
        String processed = applyArgs(resolved.text(), args);
        return switch (resolved.format()) {
            case LEGACY -> ChatColor.translateAlternateColorCodes('&', processed);
            case MINIMESSAGE -> LegacyComponentSerializer.legacySection()
                    .serialize(MINI_MESSAGE.deserialize(processed));
        };
    }

    private static Component applyAndFormatComponent(Resolved resolved, Object[] args) {
        String processed = applyArgs(resolved.text(), args);
        return switch (resolved.format()) {
            case LEGACY -> LegacyComponentSerializer.legacyAmpersand().deserialize(processed);
            case MINIMESSAGE -> MINI_MESSAGE.deserialize(processed);
        };
    }

    /**
     * Replace {0}, {1}, … placeholders with args in a single regex pass.
     * Out-of-range indices are left as literal text. Null args → "null".
     * Single-pass prevents re-substitution when an arg value itself contains {N}.
     */
    static String applyArgs(String template, Object[] args) {
        if (args == null || args.length == 0) return template;
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1));
            String replacement = idx < args.length
                    ? Matcher.quoteReplacement(String.valueOf(args[idx]))
                    : m.group(0);
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String missing(String key) {
        if (warnedKeys.add(key)) {
            warn("Missing message key: " + key);
        }
        return "<missing: " + key + ">";
    }

    private static void warn(String message) {
        if (logger != null) logger.warning("[SkGame/Messages] " + message);
    }

    // ─── Internal types ───────────────────────────────────────────────────────

    private enum Format { LEGACY, MINIMESSAGE }

    private record Resolved(Format format, String text) {}
}
