package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fire outcome for firer HUD (sanitized: {@code visitedCells == null}) or owner incoming beam
 * (full Trace path cells).
 */
public final class FireObservation {

    private final Port entryPort;
    private final Port exitPort;
    private final int colorMask;
    private final List<Cell> visitedCells;

    private FireObservation(
            Port entryPort, Port exitPort, int colorMask, List<Cell> visitedCells) {
        this.entryPort = entryPort;
        this.exitPort = exitPort;
        this.colorMask = colorMask;
        this.visitedCells = visitedCells;
    }

    /** Firer-facing: exit+color only; path always null. */
    public static FireObservation firer(Port entryPort, Port exitPort, int colorMask) {
        if (entryPort == null) {
            throw new IllegalArgumentException("entryPort must not be null");
        }
        return new FireObservation(entryPort, exitPort, colorMask, null);
    }

    /** Owner-facing: same Trace fields including full path cells. */
    public static FireObservation owner(
            Port entryPort, Port exitPort, int colorMask, List<Cell> visitedCells) {
        if (entryPort == null) {
            throw new IllegalArgumentException("entryPort must not be null");
        }
        if (visitedCells == null) {
            throw new IllegalArgumentException("visitedCells must not be null for owner view");
        }
        return new FireObservation(
                entryPort,
                exitPort,
                colorMask,
                Collections.unmodifiableList(new ArrayList<Cell>(visitedCells)));
    }

    public Port entryPort() {
        return entryPort;
    }

    /** Exit border port, or {@code null} on absorb/cycle. */
    public Port exitPort() {
        return exitPort;
    }

    public int colorMask() {
        return colorMask;
    }

    /**
     * Ordered Trace path cells for owner beam; always {@code null} on the firer-facing view.
     */
    public List<Cell> visitedCells() {
        return visitedCells;
    }
}
