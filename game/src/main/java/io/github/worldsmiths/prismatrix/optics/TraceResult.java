package io.github.worldsmiths.prismatrix.optics;

import java.util.Collections;
import java.util.List;

/**
 * Singular Trace outcome: one nullable exit port, color mask, bounce count.
 * Segments are an empty stub in M1 (presentation later).
 */
public final class TraceResult {

    private final Port exitPort;
    private final int colorMask;
    private final int totalBounces;
    private final List<Object> segments;

    public TraceResult(Port exitPort, int colorMask, int totalBounces) {
        this(exitPort, colorMask, totalBounces, Collections.emptyList());
    }

    public TraceResult(Port exitPort, int colorMask, int totalBounces, List<?> segments) {
        this.exitPort = exitPort;
        this.colorMask = colorMask;
        this.totalBounces = totalBounces;
        if (segments == null || segments.isEmpty()) {
            this.segments = Collections.emptyList();
        } else {
            this.segments = Collections.unmodifiableList(new java.util.ArrayList<Object>(segments));
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

    /** Presentation stub; empty in M1. */
    public List<Object> segments() {
        return segments;
    }
}
