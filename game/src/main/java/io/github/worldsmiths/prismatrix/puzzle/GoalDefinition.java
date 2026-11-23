package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.prismatrix.optics.Port;

/** Paired puzzle goal: entry port must exit at exitPort with colorMask. */
public final class GoalDefinition {

    private final Port entryPort;
    private final Port exitPort;
    private final int colorMask;

    public GoalDefinition(Port entryPort, Port exitPort, int colorMask) {
        if (entryPort == null) {
            throw new IllegalArgumentException("entryPort must not be null");
        }
        if (exitPort == null) {
            throw new IllegalArgumentException("exitPort must not be null");
        }
        this.entryPort = entryPort;
        this.exitPort = exitPort;
        this.colorMask = colorMask;
    }

    public Port entryPort() {
        return entryPort;
    }

    public Port exitPort() {
        return exitPort;
    }

    public int colorMask() {
        return colorMask;
    }
}
