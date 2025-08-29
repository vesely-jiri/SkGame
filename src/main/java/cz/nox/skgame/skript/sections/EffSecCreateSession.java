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
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.registrations.EventValues;
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

    static {
        Skript.registerSection(EffSecCreateSession.class,
                "create [new] [game] session (with|from) id %string%");
//        EventValues.registerEventValue(SessionCreateEvent.class, Session.class, SessionCreateEvent::getSession);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean isDelayed, ParseResult parseResult,
                        @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> triggerItems) {
        this.id = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event event) {
        String id = this.id.getSingle(event);
        if (sessionManager.getSessionById(id) == null) {
            sessionManager.createSession(id);
        }
        return super.walk(event,false);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "create session with id " + id;
    }
}