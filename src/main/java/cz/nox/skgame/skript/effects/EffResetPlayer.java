package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.SkGame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Player - Reset For Game")
@Description({
        "Fully resets a player's state for a minigame.",
        "Clears inventory, potion effects, fire, dismounts vehicles,",
        "resets health/max health/food/hunger/exp/levels/speeds/gamemode/flight/title.",
        "Gamemode is set to the value of 'defaults.gamemode' in config.yml (default: adventure).",
})
@Examples({
        "reset players of {_session} for game",
        "reset player for game"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffResetPlayer extends Effect {

    private Expression<Player> players;

    static {
        Skript.registerEffect(EffResetPlayer.class,
                "reset %players% (for|in) game"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.players = (Expression<Player>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Player[] players = this.players.getArray(event);
        if (players.length == 0) return;
        var gameMode = SkGame.getInstance().getDefaultGameMode();
        for (Player player : players) {
            resetPlayer(player, gameMode);
        }
    }

    private void resetPlayer(Player player, org.bukkit.GameMode gameMode) {
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.leaveVehicle();
        player.getInventory().clear();
        player.resetMaxHealth();
        player.setHealth(player.getMaxHealth());
        player.setExp(0f);
        player.setLevel(0);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setGameMode(gameMode);
        player.resetTitle();
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "reset " + this.players.toString(event, b) + " for game";
    }
}
