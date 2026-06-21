package cz.nox.skgame.api.game.model.type;

import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

public enum NametagVisibility {
    ALWAYS(Team.OptionStatus.ALWAYS),
    HIDE_FOR_OTHER_TEAMS(Team.OptionStatus.FOR_OTHER_TEAMS),
    HIDE_FOR_OWN_TEAM(Team.OptionStatus.FOR_OWN_TEAM),
    NEVER(Team.OptionStatus.NEVER);

    private final Team.OptionStatus optionStatus;

    NametagVisibility(Team.OptionStatus status) {
        this.optionStatus = status;
    }

    public Team.OptionStatus toOptionStatus() { return optionStatus; }

    public static @Nullable NametagVisibility fromString(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase().replace("-", "_").replace(" ", "_")) {
            case "always"                -> ALWAYS;
            case "hide_for_other_teams"  -> HIDE_FOR_OTHER_TEAMS;
            case "hide_for_own_team"     -> HIDE_FOR_OWN_TEAM;
            case "never"                 -> NEVER;
            default                      -> null;
        };
    }
}
