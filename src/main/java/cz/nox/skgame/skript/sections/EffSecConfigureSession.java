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
import cz.nox.skgame.api.game.event.SessionConfigureEvent;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Name("Configure Session")
@Description({
        "Configures an existing session by executing a block of property setters with event-session bound to it.",
        "",
        "Useful for bulk-setting session properties (shuffle, visibility, rounds, minigame, map) in one block.",
        "",
        "Supports: SECTION (code block).",
        "Supports: event-session"
})
@Examples({
        "configure session of (session of player):",
        "\tset shuffle of event-session to true",
        "\tset session rounds of event-session to 3",
        "\tset session visibility of event-session to private"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSecConfigureSession extends EffectSection {

    private Expression<Session> session;
    private Trigger trigger;

    static {
        Skript.registerSection(EffSecConfigureSession.class,
                "configure [the] session [of] %session%");
        EventValues.registerEventValue(SessionConfigureEvent.class, Session.class,
                SessionConfigureEvent::getSession);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean isDelayed, ParseResult parseResult,
                        SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
        if (hasSection()) {
            this.trigger = loadCode(sectionNode, "session configure", SessionConfigureEvent.class);
        }
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event event) {
        Session s = this.session.getSingle(event);
        if (s == null) {
            Skript.debug("configure session: session is null, skipping");
            return super.walk(event, false);
        }

        Object localVars = Variables.copyLocalVariables(event);

        if (hasSection()) {
            SessionConfigureEvent configEvent = new SessionConfigureEvent(s);
            Variables.setLocalVariables(configEvent, localVars);
            TriggerItem.walk(trigger, configEvent);
            Variables.setLocalVariables(event, Variables.copyLocalVariables(configEvent));
            Variables.removeLocals(configEvent);
        }

        return super.walk(event, false);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "configure session " + session.toString(event, debug);
    }
}
