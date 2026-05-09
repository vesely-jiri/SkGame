package cz.nox.skgame.core.module.modules;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.module.SkGameModule;

/** Phase 6: messages.yml, locale, message lookup API. Phase 5: empty shell. */
public class MessagesModule implements SkGameModule {

    @Override public String getId() { return "messages"; }
    @Override public boolean canEnable(SkGame plugin) { return true; }
    @Override public void onEnable(SkGame plugin) {}
}
