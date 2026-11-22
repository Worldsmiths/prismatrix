package io.github.worldsmiths.prismatrix.pvp;

/**
 * Outcome of a kernel verb (Ready / Fire / Guess).
 */
public final class MatchActionResult {

    private final boolean accepted;
    private final String reason;

    private MatchActionResult(boolean accepted, String reason) {
        this.accepted = accepted;
        this.reason = reason;
    }

    public static MatchActionResult ok() {
        return new MatchActionResult(true, null);
    }

    public static MatchActionResult refused(String reason) {
        return new MatchActionResult(false, reason);
    }

    public boolean accepted() {
        return accepted;
    }

    public String reason() {
        return reason;
    }
}
