package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.prismatrix.optics.PieceType;

/** Immutable fixed décor piece seeded onto the puzzle board (budget-exempt). */
public final class FixedPieceDefinition {

    private final int x;
    private final int y;
    private final PieceType type;

    public FixedPieceDefinition(int x, int y, PieceType type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public PieceType type() {
        return type;
    }
}
