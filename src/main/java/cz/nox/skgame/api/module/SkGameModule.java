package cz.nox.skgame.api.module;

import cz.nox.skgame.SkGame;

import java.util.List;
import java.util.Set;

public interface SkGameModule {

    /** Stable lowercase identifier — used as config key {@code modules.<id>.enabled}. */
    String getId();

    default String getName() { return getId(); }

    /** Other module ids this module requires. Module is skipped if any dep is disabled. */
    default Set<String> getDependencies() { return Set.of(); }

    /**
     * Called before onEnable. Return false to skip this module (e.g. soft-dep plugin absent).
     * Log a reason if returning false.
     */
    boolean canEnable(SkGame plugin);

    /**
     * Called when the module is enabled. Register listeners, install resources, init state.
     * Must not throw — log and return on failure, do not register a half-initialised module.
     */
    void onEnable(SkGame plugin);

    default void onDisable(SkGame plugin) {}

    /** Fully-qualified class names of Skript syntax classes owned by this module. */
    default List<String> getSkriptClasses() { return List.of(); }

    /** Resource paths inside the JAR (under resources/) installed to plugins/SkGame/ on first run. */
    default List<String> getResourcePaths() { return List.of(); }
}
