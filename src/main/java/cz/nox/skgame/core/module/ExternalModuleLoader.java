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
    /** Extension API major version this SkGame build supports. */
    public static final int SUPPORTED_API_VERSION = 1;

    private final SkGame plugin;
    /** id → open classloader, insertion-ordered so closeAll() can reverse. */
    private final LinkedHashMap<String, URLClassLoader> loaders = new LinkedHashMap<>();

    public ExternalModuleLoader(SkGame plugin) {
        this.plugin = plugin;
    }

    /** Scan the modules folder and instantiate every valid extension. Never throws. */
    public List<SkGameModule> loadModules() {
        List<SkGameModule> modules = new ArrayList<>();

        File dir = new File(plugin.getDataFolder(), MODULES_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogUtil().warning("Could not create modules folder: " + dir);
            return modules;
        }
        File[] jars = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
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

        static Descriptor parse(YamlConfiguration yml) {
            Descriptor d = new Descriptor();
            d.id = yml.getString("id");
            d.name = yml.getString("name", d.id);
            d.main = yml.getString("main");
            d.apiVersion = yml.getInt("api-version", 1);
            d.dependencies = yml.getStringList("dependencies");
            d.authors = yml.getStringList("authors");
            d.version = yml.getString("version", "unknown");
            return d;
        }
    }
}
