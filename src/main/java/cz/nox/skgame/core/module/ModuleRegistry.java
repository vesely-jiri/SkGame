package cz.nox.skgame.core.module;

import cz.nox.skgame.api.module.SkGameModule;
import cz.nox.skgame.core.module.modules.AdminModule;
import cz.nox.skgame.core.module.modules.DatabaseModule;
import cz.nox.skgame.core.module.modules.DefaultMinigamesModule;
import cz.nox.skgame.core.module.modules.GuiModule;
import cz.nox.skgame.core.module.modules.MessagesModule;
import cz.nox.skgame.core.module.modules.ScoreboardModule;
import java.util.List;

/** Static registry of all built-in modules. Adding a module = add an entry here. */
public final class ModuleRegistry {

    public static final List<SkGameModule> BUILTIN_MODULES = List.of(
            new GuiModule(),
            new AdminModule(),
            new DefaultMinigamesModule(),
            new MessagesModule(),
            new DatabaseModule(),
            new ScoreboardModule()
    );

    private ModuleRegistry() {}
}
