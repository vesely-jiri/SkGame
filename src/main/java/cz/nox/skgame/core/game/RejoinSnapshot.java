package cz.nox.skgame.core.game;

import java.util.UUID;

public record RejoinSnapshot(UUID playerUuid, String sessionId, long disconnectTime) {}
