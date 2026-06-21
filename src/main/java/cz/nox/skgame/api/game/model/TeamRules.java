package cz.nox.skgame.api.game.model;

import cz.nox.skgame.api.game.model.type.NametagVisibility;

/** Fully-resolved team rule set — all fields non-null. */
public record TeamRules(boolean friendlyFire, boolean collision, NametagVisibility nametag) {
    public static final TeamRules DEFAULT = new TeamRules(true, true, NametagVisibility.ALWAYS);
}
