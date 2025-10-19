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
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Name("Create Game Session")
@Description("Creates a game session")
@Examples({
    "create game session with id \"hello\"",
    "create game session:",
        "\tset players of event-session to {_players::*}"})
@Since("3.12")
@SuppressWarnings("unused")
public class EffSecCreateSession extends EffectSection {

    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<String> id;
    private Trigger trigger;

    static {
        Skript.registerSection(EffSecCreateSession.class,
                "create [new] [game] session (with|from) id %string%");
        EventValues.registerEventValue(SessionCreateEvent.class, Session.class, SessionCreateEvent::getSession);
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
        String id = this.id.getSingle(e);
        Session session = sessionManager.getSessionById(id);
        if (session == null) {
            session = sessionManager.createSession(id);
        }
        if (hasSection()) {
            SessionCreateEvent createEvent = new SessionCreateEvent(session);
            Variables.setLocalVariables(createEvent,localVars);
            TriggerItem.walk(this.trigger,createEvent);
            Variables.setLocalVariables(e,Variables.copyLocalVariables(createEvent));
            Variables.removeLocals(createEvent);
        }
        return super.walk(e,false);
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return "create session with id " + this.id.toString(e,b);
    }
}