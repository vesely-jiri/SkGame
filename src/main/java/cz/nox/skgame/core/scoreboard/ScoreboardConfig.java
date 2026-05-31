package cz.nox.skgame.core.scoreboard;

import java.util.ArrayList;
import java.util.List;

/** Immutable snapshot of scoreboard.yml layout. */
public record ScoreboardConfig(
        String title,
        List<String> headerLines,
        List<String> footerLines
) {
    /** Lines before {content} marker. */
    public static ScoreboardConfig parse(String title, List<String> rawLines) {
        int markerIdx = -1;
        for (int i = 0; i < rawLines.size(); i++) {
            if ("{content}".equals(rawLines.get(i).trim())) { markerIdx = i; break; }
        }
        if (markerIdx == -1) {
            // No marker — treat all as header, content will be appended at end
            return new ScoreboardConfig(title, List.copyOf(rawLines), List.of());
        }
        List<String> header = new ArrayList<>(rawLines.subList(0, markerIdx));
        List<String> footer = new ArrayList<>(rawLines.subList(markerIdx + 1, rawLines.size()));
        return new ScoreboardConfig(title, List.copyOf(header), List.copyOf(footer));
    }

    /** True when scoreboard.yml had no {content} marker — content appended at end. */
    public boolean isContentAppended() { return footerLines.isEmpty() && headerLines.size() == footerLines.size(); }

    public static ScoreboardConfig defaultConfig() {
        return new ScoreboardConfig("&6Event Server", List.of("&6----------------"), List.of("&6----------------"));
    }
}
