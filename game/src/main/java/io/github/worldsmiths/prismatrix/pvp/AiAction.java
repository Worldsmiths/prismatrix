package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.Port;

/**
 * Play verb chosen by an {@link AiPolicy}: Fire XOR Guess. Applied through the same kernel paths
 * as a human seat.
 */
public final class AiAction {

    public enum Kind {
        FIRE,
        GUESS
    }

    private final Kind kind;
    private final Port port;
    private final BoardState hypothesis;

    private AiAction(Kind kind, Port port, BoardState hypothesis) {
        this.kind = kind;
        this.port = port;
        this.hypothesis = hypothesis;
    }

    public static AiAction fire(Port port) {
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }
        return new AiAction(Kind.FIRE, port, null);
    }

    public static AiAction guess(BoardState hypothesis) {
        if (hypothesis == null) {
            throw new IllegalArgumentException("hypothesis must not be null");
        }
        return new AiAction(Kind.GUESS, null, hypothesis);
    }

    public Kind kind() {
        return kind;
    }

    /** Non-null when {@link Kind#FIRE}. */
    public Port port() {
        return port;
    }

    /** Non-null when {@link Kind#GUESS}. */
    public BoardState hypothesis() {
        return hypothesis;
    }
}
