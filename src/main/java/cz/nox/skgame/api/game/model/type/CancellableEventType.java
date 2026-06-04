package cz.nox.skgame.api.game.model.type;

public enum CancellableEventType {
    DAMAGE,
    PVP,
    FALL_DAMAGE,
    HUNGER,
    ITEM_DROP,
    ITEM_PICKUP,
    BLOCK_BREAK,
    BLOCK_PLACE,
    PORTAL;

    public String skriptName() {
        return name().toLowerCase().replace('_', '-');
    }
}
