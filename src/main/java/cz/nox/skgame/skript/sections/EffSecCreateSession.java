package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@Name("Create Game Session")
@Description({
        "Creates a new game session, optionally with a specific id.",
        "When no id is given, a random UUID is generated.",
        "If a session with the given id already exists, the existing session is returned silently.",
        "",
        "Sessions are transient runtime objects — not persisted across server restarts.",
        "The 'register' verb is intentionally absent; contrast 'register minigame' / 'register map',",
        "which are persistent. Use 'create session' only.",
        "",
        "Supports: SECTION (code block). event-session is available inside the block."
})
@Examples({
        "# Create a session — auto-generated id",
        "create game session",
        "",
        "# Create with explicit id",
        "create game session with id \"arena_1\"",
        "",
        "# Create with callback block — event-session is the newly created session",
        "create new game session with id \"lobby_room\":",
        "    set minigame of event-session to minigame with id \"koth\"",
        "    set map of event-session to gamemap with id \"arena_main\"",
        "    set shuffle of event-session to true",
        "    set session rounds of event-session to 3"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSecCreateSession extends EffectSection {

    private static final SessionManager sessionManager = SessionManager.getInstance();
    @Nullable private Expression<String> id;
    private Trigger trigger;

    static {
        Skript.registerSection(EffSecCreateSession.class,
                "create [new] [game] session [(with|from) [uu]id %string%]");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean isDelayed, ParseResult parseResult,
                        SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
        if (hasSection()) {
            this.trigger = loadCode(sectionNode, "session create", SessionCreateEvent.class);
        }
        this.id = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event e) {
        Object localVars = Variables.copyLocalVariables(e);
        String rawId = this.id != null ? this.id.getSingle(e) : null;
        String id = rawId != null ? rawId : UUID.randomUUID().toString();
        Session session = sessionManager.getSessionById(id);
        boolean justCreated = (session == null);
        if (justCreated) {
            session = sessionManager.registerSession(id);
        }
        // Fire public event exactly once for newly created sessions (host stays null for Skript-created sessions)
        SessionCreateEvent createEvent = new SessionCreateEvent(session);
        if (justCreated) {
            Bukkit.getPluginManager().callEvent(createEvent);
        }
        if (hasSection()) {
            Variables.setLocalVariables(createEvent, localVars); // reuse as section context for event-session
            TriggerItem.walk(this.trigger, createEvent);
            Variables.setLocalVariables(e, Variables.copyLocalVariables(createEvent));
            Variables.removeLocals(createEvent);
        }
        return super.walk(e, false);
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return "create session" + (this.id != null ? " with id " + this.id.toString(e, b) : "");
    }
}
