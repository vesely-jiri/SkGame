package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.EntryNode;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.locale.ScriptLocaleRegistry;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos.Structure.NodeType;

import java.util.LinkedHashMap;
import java.util.Map;

@Name("Script Locale")
@Description({
        "Defines locale strings for a script namespace at load time.",
        "Locale codes accept short form (cs, en) or full form (cs_CZ, en_US).",
        "Fallback chain: forced-locale → player locale → language-only → en_US → en.",
        "Use 'send locale \"ns:key\" to player' to send a localized message.",
})
@Examples({
        "# Define locale strings for a namespace — used via send locale / locale expression",
        "locale \"bomberman\":",
        "    game.start:",
        "        cs: \"Hra začala!\"",
        "        en: \"Game started!\"",
        "    bomb.placed:",
        "        cs: \"Umístil jsi bombu!\"",
        "        en: \"You placed a bomb!\"",
        "    winner:",
        "        cs: \"Vyhrál hráč %s!\"",
        "        en: \"Player %s won!\"",
        "",
        "# Then in event handlers:",
        "on \"bomberman\" game start:",
        "    send locale \"bomberman:game.start\" to session players of event-session",
        "    send title locale \"bomberman:game.start\" to session players of event-session",
        "    send actionbar locale \"bomberman:game.start\" to session players of event-session",
        "",
        "# Or use as expression in format strings:",
        "    broadcast locale \"bomberman:game.start\" for event-player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class StrucLocale extends Structure {

    private Literal<String> namespace;
    private Map<String, Map<String, String>> parsedEntries;

    static {
        // null entryValidator → Skript passes EntryContainer.withoutValidator; all nodes are dynamic
        Skript.registerStructure(StrucLocale.class, null, NodeType.SECTION,
                "locale %string%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult,
                        @Nullable EntryContainer entryContainer) {
        this.namespace = (Literal<String>) args[0];
        if (entryContainer == null) return false;

        parsedEntries = new LinkedHashMap<>();
        SectionNode source = entryContainer.getSource();

        for (Node keyNode : source) {
            String messageKey = stripQuotes(keyNode.getKey());
            if (messageKey == null || messageKey.isEmpty()) continue;
            if (!(keyNode instanceof SectionNode localeSection)) continue;

            Map<String, String> localeMap = new LinkedHashMap<>();
            for (Node localeNode : localeSection) {
                String localeCode, rawText;
                if (localeNode instanceof EntryNode entryNode) {
                    localeCode = Messages.normalize(entryNode.getKey());
                    rawText = entryNode.getValue();
                } else if (localeNode instanceof SimpleNode) {
                    // Skript script configs use simple=true/separator=":" so "cs: text" becomes
                    // SimpleNode (line doesn't end with ":"), not EntryNode — split manually.
                    String raw = localeNode.getKey();
                    int sep = raw.indexOf(':');
                    if (sep < 0) continue;
                    localeCode = Messages.normalize(raw.substring(0, sep).trim());
                    rawText = raw.substring(sep + 1).trim();
                } else {
                    continue;
                }
                String text = stripQuotes(rawText);
                if (text != null && !text.isEmpty()) localeMap.put(localeCode, text);
            }
            parsedEntries.put(messageKey, localeMap);
        }
        return true;
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public boolean postLoad() {
        String ns = namespace.getSingle(null);
        if (ns == null) return false;
        ScriptLocaleRegistry.getInstance().register(ns, parsedEntries);
        return true;
    }

    @Override
    public void unload() {
        String ns = namespace.getSingle(null);
        if (ns != null) ScriptLocaleRegistry.getInstance().unregister(ns);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "locale " + namespace.toString(event, debug);
    }

    /** Strip surrounding double or single quotes from raw config node strings. */
    private static @Nullable String stripQuotes(@Nullable String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.length() >= 2
                && ((s.startsWith("\"") && s.endsWith("\""))
                || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
