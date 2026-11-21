package io.github.worldsmiths.prismatrix.sandbox;

import io.github.worldsmiths.prismatrix.optics.Port;

/** Per-observation consistency badge after Ghost Trace. */
public final class ConsistencyBadge {

    public enum Kind {
        NEUTRAL,
        GREEN,
        RED
    }

    private final Port entryPort;
    private final Kind kind;

    public ConsistencyBadge(Port entryPort, Kind kind) {
        if (entryPort == null) {
            throw new IllegalArgumentException("entryPort must not be null");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        this.entryPort = entryPort;
        this.kind = kind;
    }

    public Port entryPort() {
        return entryPort;
    }

    public Kind kind() {
        return kind;
    }
}
