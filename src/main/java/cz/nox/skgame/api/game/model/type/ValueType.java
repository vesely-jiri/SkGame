package cz.nox.skgame.api.game.model.type;

/**
 * Native, Skript-independent type token for a {@link cz.nox.skgame.api.game.model.CustomValue}.
 *
 * <p>Replaces the Skript {@code ClassInfo<?>} as the authoritative type for (de)serialization and
 * GUI editing, so typed value defs (locations, regions, numbers …) work in standalone mode. In
 * addon mode a {@code SkriptTypeAdapter} maps each constant to/from the matching Skript
 * {@code ClassInfo} so existing Skript syntax ({@code type: a location}) keeps working unchanged.
 *
 * <p>The {@link #getCode() code} is the stable on-disk identifier — never rename it once shipped.
 */
public enum ValueType {
    TEXT("text"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    LOCATION("location"),
    BLOCK("block"),
    ITEM("item"),
    REGION("region"),
    PLAYER("player"),
    TIMESPAN("timespan");

    private final String code;

    ValueType(String code) {
        this.code = code;
    }

    /** Stable serialization identifier. */
    public String getCode() {
        return code;
    }

    /** Resolve from a {@link #getCode() code} (or the enum name), case-insensitive. Null if unknown. */
    public static ValueType fromCode(String code) {
        if (code == null) return null;
        for (ValueType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        try {
            return ValueType.valueOf(code.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
