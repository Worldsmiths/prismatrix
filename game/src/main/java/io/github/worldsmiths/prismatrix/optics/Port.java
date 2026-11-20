package io.github.worldsmiths.prismatrix.optics;

/**
 * Border port: side {@code T|B|L|R} plus index {@code 0..N-1}.
 *
 * <p>Entry cell / direction (HLD Appendix A):
 * <ul>
 *   <li>{@code T{i}} → cell {@code (i,0)} traveling {@link Direction#DOWN}
 *   <li>{@code B{i}} → cell {@code (i,N-1)} traveling {@link Direction#UP}
 *   <li>{@code L{i}} → cell {@code (0,i)} traveling {@link Direction#RIGHT}
 *   <li>{@code R{i}} → cell {@code (N-1,i)} traveling {@link Direction#LEFT}
 * </ul>
 */
public final class Port {

    public enum Side {
        T,
        B,
        L,
        R
    }

    private final Side side;
    private final int index;

    private Port(Side side, int index) {
        this.side = side;
        this.index = index;
    }

    /**
     * Creates a port validated against board size {@code boardSize}.
     *
     * @throws IllegalArgumentException if side is null or index is out of {@code [0, boardSize)}
     */
    public static Port of(Side side, int index, int boardSize) {
        if (side == null) {
            throw new IllegalArgumentException("port side must not be null");
        }
        if (boardSize < 1) {
            throw new IllegalArgumentException("board size must be >= 1, got " + boardSize);
        }
        if (index < 0 || index >= boardSize) {
            throw new IllegalArgumentException(
                    "port index " + index + " out of bounds for size " + boardSize);
        }
        return new Port(side, index);
    }

    public Side side() {
        return side;
    }

    public int index() {
        return index;
    }

    /** Entry cell X for a board of the given size. */
    public int entryX(int boardSize) {
        switch (side) {
            case T:
            case B:
                return index;
            case L:
                return 0;
            case R:
                return boardSize - 1;
            default:
                throw new IllegalStateException("unknown side " + side);
        }
    }

    /** Entry cell Y for a board of the given size. */
    public int entryY(int boardSize) {
        switch (side) {
            case T:
                return 0;
            case B:
                return boardSize - 1;
            case L:
            case R:
                return index;
            default:
                throw new IllegalStateException("unknown side " + side);
        }
    }

    /** Direction of travel when entering from this port. */
    public Direction entryDirection() {
        switch (side) {
            case T:
                return Direction.DOWN;
            case B:
                return Direction.UP;
            case L:
                return Direction.RIGHT;
            case R:
                return Direction.LEFT;
            default:
                throw new IllegalStateException("unknown side " + side);
        }
    }

    /**
     * Port on the opposite border at the same index — empty-board straight-through exit
     * (e.g. 3×3 {@code T1} exits {@code B1}).
     */
    public Port opposite() {
        Side opp;
        switch (side) {
            case T:
                opp = Side.B;
                break;
            case B:
                opp = Side.T;
                break;
            case L:
                opp = Side.R;
                break;
            case R:
                opp = Side.L;
                break;
            default:
                throw new IllegalStateException("unknown side " + side);
        }
        return new Port(opp, index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Port)) {
            return false;
        }
        Port other = (Port) o;
        return side == other.side && index == other.index;
    }

    @Override
    public int hashCode() {
        return 31 * side.hashCode() + index;
    }

    @Override
    public String toString() {
        return side.name() + index;
    }
}
