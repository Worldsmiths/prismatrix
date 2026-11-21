package io.github.worldsmiths.prismatrix.optics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singular Trace outcome: one nullable exit port, color mask, bounce count, and
 * ordered visited cells (walk order).
 */
public final class TraceResult {

    private final Port exitPort;
    private final int colorMask;
    private final int totalBounces;
    private final List<Cell> visitedCells;

    public TraceResult(Port exitPort, int colorMask, int totalBounces) {
        this(exitPort, colorMask, totalBounces, Collections.<Cell>emptyList());
    }

    public TraceResult(Port exitPort, int colorMask, int totalBounces, List<Cell> visitedCells) {
        this.exitPort = exitPort;
        this.colorMask = colorMask;
        this.totalBounces = totalBounces;
        if (visitedCells == null || visitedCells.isEmpty()) {
            this.visitedCells = Collections.emptyList();
        } else {
            this.visitedCells = Collections.unmodifiableList(new ArrayList<Cell>(visitedCells));
        }
    }

    /** Exit border port, or {@code null} on absorb/cycle. */
    public Port exitPort() {
        return exitPort;
    }

    public int colorMask() {
        return colorMask;
    }

    public int totalBounces() {
        return totalBounces;
    }

    /**
     * Ordered cells the ray occupies in walk order (entry through last interior cell).
     * Replaces the M1 empty {@code segments} stub for Neutral / beams.
     */
    public List<Cell> visitedCells() {
        return visitedCells;
    }
}
