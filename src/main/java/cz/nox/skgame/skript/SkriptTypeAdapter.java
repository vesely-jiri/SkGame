package cz.nox.skgame.skript;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Timespan;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.type.ValueType;
import cz.nox.skgame.api.region.Region;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Addon-only bridge between the native {@link ValueType} and Skript's {@code ClassInfo<?>}.
 *
 * <p>{@link #install()} is called once in addon mode (never in standalone), wiring the mappings into
 * {@link CustomValue} so: Skript syntax ({@code type: a location}) that calls
 * {@link CustomValue#setType(ClassInfo)} auto-derives the native {@code ValueType}; and native defs
 * created by extensions ({@code setType(ValueType.LOCATION)}) expose the right {@code ClassInfo} to
 * Skript value expressions. This class references {@code ch.njol.skript.*} and must therefore only
 * ever be loaded in addon mode.
 */
public final class SkriptTypeAdapter {

    private SkriptTypeAdapter() {}

    /** Wire ValueType ↔ ClassInfo into CustomValue. Call once, addon mode only. */
    public static void install() {
        CustomValue.installSkriptTypeBridge(SkriptTypeAdapter::toClassInfo, SkriptTypeAdapter::toValueType);
    }

    /** Native type → Skript ClassInfo (by stable Skript code name). */
    public static @Nullable ClassInfo<?> toClassInfo(@Nullable ValueType vt) {
        if (vt == null) return null;
        String code = switch (vt) {
            case TEXT -> "string";
            case NUMBER -> "number";
            case BOOLEAN -> "boolean";
            case LOCATION -> "location";
            case BLOCK -> "block";
            case ITEM -> "itemstack";
            case REGION -> "skarena";
            case PLAYER -> "player";
            case TIMESPAN -> "timespan";
        };
        return Classes.getClassInfoNoError(code);
    }

    /** Skript ClassInfo → native type (by code name, with a Class fallback). */
    public static @Nullable ValueType toValueType(@Nullable ClassInfo<?> ci) {
        if (ci == null) return null;
        String code = ci.getCodeName();
        if (code != null) {
            switch (code.toLowerCase()) {
                case "string", "text" -> { return ValueType.TEXT; }
                case "number", "integer", "long", "double", "float", "short", "byte" -> { return ValueType.NUMBER; }
                case "boolean" -> { return ValueType.BOOLEAN; }
                case "location" -> { return ValueType.LOCATION; }
                case "block" -> { return ValueType.BLOCK; }
                case "itemstack", "itemtype", "item", "slot" -> { return ValueType.ITEM; }
                case "skarena", "arena", "region" -> { return ValueType.REGION; }
                case "player", "offlineplayer" -> { return ValueType.PLAYER; }
                case "timespan" -> { return ValueType.TIMESPAN; }
                default -> { /* fall through to Class check */ }
            }
        }
        Class<?> c = ci.getC();
        if (c == null) return null;
        if (CharSequence.class.isAssignableFrom(c)) return ValueType.TEXT;
        if (Number.class.isAssignableFrom(c)) return ValueType.NUMBER;
        if (Boolean.class.isAssignableFrom(c)) return ValueType.BOOLEAN;
        if (Location.class.isAssignableFrom(c)) return ValueType.LOCATION;
        if (Region.class.isAssignableFrom(c)) return ValueType.REGION;
        if (ItemStack.class.isAssignableFrom(c)) return ValueType.ITEM;
        if (Block.class.isAssignableFrom(c)) return ValueType.BLOCK;
        if (Player.class.isAssignableFrom(c)) return ValueType.PLAYER;
        if (Timespan.class.isAssignableFrom(c)) return ValueType.TIMESPAN;
        return null;
    }
}
