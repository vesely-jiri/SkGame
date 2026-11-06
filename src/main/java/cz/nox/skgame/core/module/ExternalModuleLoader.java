package cz.nox.skgame.core.module;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads external native extensions — jars dropped in {@code plugins/SkGame/modules/}.
 *
 * <p>Each jar must contain a {@code skgame-extension.yml} descriptor at its root
 * (see docs/extension-api.md). Each is loaded with a {@link URLClassLoader} whose
 * parent is SkGame's own classloader, so {@code cz.nox.skgame.api.*} types resolve to
 * the same {@link Class} objects across the boundary (casts / {@code instanceof} work).
 *
 * <p>This loader only instantiates the modules. They are enabled through the same
 * pipeline as {@link ModuleRegistry#BUILTIN_MODULES} (config gate → deps → canEnable →
 * resource install → onEnable), driven by {@code SkGame.resolveModules()}.
 */
public final class ExternalModuleLoader {

    /** Descriptor file name at the jar root. Distinct from Bukkit's plugin.yml. */
    public static final String DESCRIPTOR = "skgame-extension.yml";
    /** Folder scanned for extension jars, relative to the plugin data folder. */
    public static final String MODULES_DIR = "modules";
    /** Default config template resource read from the module jar root when the descriptor omits {@code config}. */
    public static final String DEFAULT_CONFIG_RESOURCE = "config.yml";
    /** Extension API major version this SkGame build supports. */
    public static final int SUPPORTED_API_VERSION = 1;

    private final SkGame plugin;
    /** Absolute {@code plugins/SkGame/modules} folder — holds both the jars and the generated {@code <id>.yml}. */
    private final File modulesDir;
    /** id → open classloader, insertion-ordered so closeAll() can reverse. */
    private final LinkedHashMap<String, URLClassLoader> loaders = new LinkedHashMap<>();
    /** lowercased id → loaded module config (never written back, so on-disk comments/edits are preserved). */
    private final LinkedHashMap<String, YamlConfiguration> configs = new LinkedHashMap<>();
    /** lowercased id → the <id>.yml file, kept for reloadConfigs(). */
    private final LinkedHashMap<String, File> configFiles = new LinkedHashMap<>();
    /** id → the jar it was loaded from, for change-detection and hot-reload. */
    private final LinkedHashMap<String, File> jarFiles = new LinkedHashMap<>();
    /** id → jar mtime at load time, to detect a rebuilt jar on {@code /skgame reload modules}. */
    private final LinkedHashMap<String, Long> jarMtimes = new LinkedHashMap<>();

    public ExternalModuleLoader(SkGame plugin) {
        this.plugin = plugin;
        this.modulesDir = new File(plugin.getDataFolder(), MODULES_DIR);
    }

    /** Scan the modules folder and instantiate every valid extension. Never throws. */
    public List<SkGameModule> loadModules() {
        List<SkGameModule> modules = new ArrayList<>();

        if (!modulesDir.exists() && !modulesDir.mkdirs()) {
            plugin.getLogUtil().warning("Could not create modules folder: " + modulesDir);
            return modules;
        }
        File[] jars = modulesDir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null || jars.length == 0) return modules;

        for (File jar : jars) {
            SkGameModule module = loadOne(jar);
            if (module != null) modules.add(module);
        }
        return modules;
    }

    private SkGameModule loadOne(File jar) {
        Descriptor desc;
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry(DESCRIPTOR);
            if (entry == null) {
                plugin.getLogUtil().warning("Skipping '" + jar.getName()
                        + "': no " + DESCRIPTOR + " at jar root");
                return null;
            }
            try (InputStream in = jarFile.getInputStream(entry);
                 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                desc = Descriptor.parse(YamlConfiguration.loadConfiguration(reader));
            }
        } catch (IOException e) {
            plugin.getLogUtil().error("Failed to read '" + jar.getName() + "': " + e.getMessage());
            return null;
        }

        if (desc.id == null || desc.id.isBlank() || desc.main == null || desc.main.isBlank()) {
            plugin.getLogUtil().error("Invalid " + DESCRIPTOR + " in '" + jar.getName()
                    + "': 'id' and 'main' are required");
            return null;
        }
        if (loaders.containsKey(desc.id)) {
            plugin.getLogUtil().error("Duplicate extension id '" + desc.id
                    + "' from '" + jar.getName() + "' — skipping (already loaded)");
            return null;
        }
        if (desc.apiVersion > SUPPORTED_API_VERSION) {
            plugin.getLogUtil().error("Extension '" + desc.id + "' targets API v" + desc.apiVersion
                    + " but this SkGame supports v" + SUPPORTED_API_VERSION + " — skipping");
            return null;
        }

        URLClassLoader cl = null;
        try {
            URL jarUrl = jar.toURI().toURL();
            cl = new URLClassLoader(new URL[]{jarUrl}, SkGame.class.getClassLoader());
            Class<?> mainClass = Class.forName(desc.main, true, cl);
            if (!SkGameModule.class.isAssignableFrom(mainClass)) {
                plugin.getLogUtil().error("Extension '" + desc.id + "' main class " + desc.main
                        + " does not implement SkGameModule — skipping");
                cl.close();
                return null;
            }
            SkGameModule module = (SkGameModule) mainClass.getDeclaredConstructor().newInstance();
            if (!module.getId().equalsIgnoreCase(desc.id)) {
                plugin.getLogUtil().warning("Extension '" + jar.getName() + "': descriptor id '"
                        + desc.id + "' != module.getId() '" + module.getId()
                        + "'. Using module id for the enable pipeline.");
            }
            loaders.put(desc.id, cl);
            jarFiles.put(desc.id, jar);
            jarMtimes.put(desc.id, jar.lastModified());
            // Generate (first run) + load the module's editable config, next to the jar.
            setupModuleConfig(module, jar, desc);
            plugin.getLogUtil().info("Loaded extension '" + desc.id + "' ("
                    + desc.name + ") from " + jar.getName());
            return module;
        } catch (Throwable t) {
            plugin.getLogUtil().error("Failed to load extension '" + desc.id + "' from '"
                    + jar.getName() + "': " + t);
            if (cl != null) {
                try { cl.close(); } catch (IOException ignored) {}
            }
            return null;
        }
    }

    /**
     * Generate (on first run) and load a module's config at {@code plugins/SkGame/modules/<id>.yml}
     * — the SAME folder as the jars. Template source: the jar resource named by the descriptor's
     * {@code config:} field, else {@value #DEFAULT_CONFIG_RESOURCE} at the jar root; if neither exists,
     * a short generated header so the file still appears and is editable. An existing file is NEVER
     * overwritten (user edits + comments are preserved). The loaded {@link YamlConfiguration} is never
     * written back, so on-disk comments survive.
     */
    private void setupModuleConfig(SkGameModule module, File jar, Descriptor desc) {
        String id = module.getId();
        File target = new File(modulesDir, id + ".yml");

        if (!target.exists()) {
            String templatePath = (desc.config != null && !desc.config.isBlank())
                    ? desc.config : DEFAULT_CONFIG_RESOURCE;
            boolean copied = false;
            try (JarFile jarFile = new JarFile(jar)) {
                JarEntry entry = jarFile.getJarEntry(templatePath);
                if (entry != null) {
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        java.nio.file.Files.copy(in, target.toPath());
                        copied = true;
                        plugin.getLogUtil().info("Generated config '" + target.getName()
                                + "' for extension '" + id + "' from jar resource '" + templatePath + "'");
                    }
                }
            } catch (IOException e) {
                plugin.getLogUtil().error("Failed to copy config template for '" + id + "': " + e.getMessage());
            }
            if (!copied) {
                String header = "# Config for " + desc.name + " (" + id + ")\n"
                        + "# Auto-generated by SkGame on first load. Edit freely — SkGame never overwrites this file.\n";
                try {
                    java.nio.file.Files.writeString(target.toPath(), header, StandardCharsets.UTF_8);
                    plugin.getLogUtil().info("Generated empty config '" + target.getName()
                            + "' for extension '" + id + "' (no template in jar)");
                } catch (IOException e) {
                    plugin.getLogUtil().error("Failed to create config for '" + id + "': " + e.getMessage());
                    return;
                }
            }
        }

        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.load(target);
        } catch (Exception e) {
            plugin.getLogUtil().error("Failed to load config '" + target.getName()
                    + "': " + e.getMessage() + " — exposing an empty config to '" + id + "'");
        }
        configs.put(id.toLowerCase(), cfg);
        configFiles.put(id.toLowerCase(), target);
    }

    /**
     * The loaded config for an external module (by id, case-insensitive), or {@code null} if that id
     * is not a loaded external extension. Never writes; edit the file and {@code /skgame reload}.
     */
    public YamlConfiguration getConfig(String moduleId) {
        return moduleId == null ? null : configs.get(moduleId.toLowerCase());
    }

    /**
     * Re-read every module config from disk INTO THE SAME {@link YamlConfiguration} object, so a module
     * that cached the reference from {@code getModuleConfig(...)} sees the updated values. Never
     * regenerates or overwrites the file.
     */
    public void reloadConfigs() {
        for (Map.Entry<String, YamlConfiguration> e : configs.entrySet()) {
            File f = configFiles.get(e.getKey());
            if (f == null || !f.exists()) continue;
            try {
                e.getValue().load(f);
                plugin.getLogUtil().info("Reloaded module config: " + f.getName());
            } catch (Exception ex) {
                plugin.getLogUtil().error("Failed to reload config '" + f.getName() + "': " + ex.getMessage());
            }
        }
    }

    /** Peek only the descriptor {@code id} from a jar (for rescans), or {@code null} if unreadable/absent. */
    private String peekId(File jar) {
        try (JarFile jf = new JarFile(jar)) {
            JarEntry e = jf.getJarEntry(DESCRIPTOR);
            if (e == null) return null;
            try (InputStream in = jf.getInputStream(e);
                 Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(r).getString("id");
            }
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Instantiate every jar in the folder whose id is NOT currently loaded — the modules added
     * since boot (or since the last reload). Returned modules are NOT yet enabled; the caller runs
     * the enable gate (config/deps/canEnable) + onEnable. Unchanged jars are left untouched (no
     * duplicate-id noise). Never throws.
     */
    public List<SkGameModule> loadNewModules() {
        List<SkGameModule> fresh = new ArrayList<>();
        File[] jars = modulesDir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null) return fresh;
        for (File jar : jars) {
            String id = peekId(jar);
            if (id == null || loaders.containsKey(id)) continue;
            SkGameModule m = loadOne(jar);
            if (m != null) fresh.add(m);
        }
        return fresh;
    }

    /** Snapshot of currently-loaded external module ids (bookkeeping keys, descriptor-case). */
    public List<String> loadedIds() {
        return new ArrayList<>(jarFiles.keySet());
    }

    /** The stored (canonical) id matching {@code id} case-insensitively, or {@code null} if not loaded. */
    public String canonicalId(String id) {
        if (id == null) return null;
        for (String k : jarFiles.keySet()) {
            if (k.equalsIgnoreCase(id)) return k;
        }
        return null;
    }

    /** The jar a loaded id came from (case-insensitive), or {@code null}. */
    public File getLoadedJar(String id) {
        String k = canonicalId(id);
        return k == null ? null : jarFiles.get(k);
    }

    /** Loaded ids whose jar file was rebuilt on disk (mtime differs) — candidates for hot-swap. */
    public List<String> changedModuleIds() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, File> e : jarFiles.entrySet()) {
            File jar = e.getValue();
            Long seen = jarMtimes.get(e.getKey());
            if (jar != null && jar.exists() && seen != null && jar.lastModified() != seen) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    /** Loaded ids whose jar file has been deleted from the folder. */
    public List<String> removedModuleIds() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, File> e : jarFiles.entrySet()) {
            File jar = e.getValue();
            if (jar == null || !jar.exists()) out.add(e.getKey());
        }
        return out;
    }

    /**
     * Detach a loaded module so it can be re-loaded or dropped: close its classloader and forget the
     * loader/jar bookkeeping. The module's config entry is KEPT (so a module's onDisable can still read
     * {@code getModuleConfig}, and a subsequent {@link #reload(File)} re-reads it from disk). Call the
     * module's {@code onDisable} BEFORE this (its code runs fine after the loader closes — already-loaded
     * classes stay resident until GC — but closing first is cleaner).
     *
     * @return the jar the module was loaded from, or {@code null} if {@code id} was not loaded.
     */
    public File detach(String id) {
        URLClassLoader cl = loaders.remove(id);
        jarMtimes.remove(id);
        File jar = jarFiles.remove(id);
        if (cl != null) {
            try { cl.close(); } catch (IOException ignored) {}
        }
        return jar;
    }

    /**
     * Re-instantiate a module from a jar after {@link #detach(String)} — same path as the initial load
     * (fresh classloader, re-read config). Caller runs the enable gate + onEnable. Returns {@code null}
     * on failure.
     */
    public SkGameModule reload(File jar) {
        return loadOne(jar);
    }

    /** Close every open extension classloader, in reverse load order. */
    public void closeAll() {
        List<Map.Entry<String, URLClassLoader>> entries = new ArrayList<>(loaders.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<String, URLClassLoader> e : entries) {
            try {
                e.getValue().close();
            } catch (IOException ex) {
                plugin.getLogUtil().warning("Failed to close classloader for '" + e.getKey() + "': " + ex.getMessage());
            }
        }
        loaders.clear();
        configs.clear();
        configFiles.clear();
        jarFiles.clear();
        jarMtimes.clear();
    }

    /** Parsed {@code skgame-extension.yml}. */
    private static final class Descriptor {
        String id;
        String name;
        String main;
        int apiVersion;
        List<String> dependencies;
        List<String> authors;
        String version;
        /** Optional jar-resource path for the default config template; defaults to {@value #DEFAULT_CONFIG_RESOURCE}. */
        String config;

        static Descriptor parse(YamlConfiguration yml) {
            Descriptor d = new Descriptor();
            d.id = yml.getString("id");
            d.name = yml.getString("name", d.id);
            d.main = yml.getString("main");
            d.apiVersion = yml.getInt("api-version", 1);
            d.dependencies = yml.getStringList("dependencies");
            d.authors = yml.getStringList("authors");
            d.version = yml.getString("version", "unknown");
            d.config = yml.getString("config");
            return d;
        }
    }
}
