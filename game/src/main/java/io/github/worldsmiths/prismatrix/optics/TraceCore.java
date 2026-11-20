package io.github.worldsmiths.prismatrix.optics;

import java.util.HashSet;
import java.util.Set;

/**
 * Pure single-ray Trace kernel (HLD Appendix A).
 *
 * <p>Does not mutate {@link BoardState}. Cycle detection uses visited
 * {@code (x, y, dir, color)} and yields a null exit without throwing.
 */
public final class TraceCore {

    private TraceCore() {}

    /**
     * Trace from a validated border port.
     *
     * @throws IllegalArgumentException if board/port is null or port is OOB for the board
     */
    public static TraceResult trace(BoardState board, Port entry) {
        if (board == null) {
            throw new IllegalArgumentException("board must not be null");
        }
        if (entry == null) {
            throw new IllegalArgumentException("entry port must not be null");
        }
        int n = board.size();
        // Validate against this board before any stepping (fail closed).
        Port.of(entry.side(), entry.index(), n);

        return traceAt(
                board, entry.entryX(n), entry.entryY(n), entry.entryDirection(), 0);
    }

    /**
     * Single-ray step loop from an interior cell. Package-visible so the cycle golden can
     * start on a mid-grid state that border ports cannot reach with M1 mirrors alone.
     */
    static TraceResult traceAt(
            BoardState board, int startX, int startY, Direction startDir, int startColor) {
        int n = board.size();
        if (startX < 0 || startY < 0 || startX >= n || startY >= n) {
            throw new IllegalArgumentException(
                    "start cell (" + startX + "," + startY + ") out of bounds for size " + n);
        }
        if (startDir == null) {
            throw new IllegalArgumentException("start direction must not be null");
        }

        int x = startX;
        int y = startY;
        Direction dir = startDir;
        int color = startColor;
        int bounces = 0;
        Set<Long> visited = new HashSet<Long>();

        while (true) {
            long key = visitKey(x, y, dir, color);
            if (!visited.add(key)) {
                return new TraceResult(null, 0, bounces);
            }

            PieceType piece = board.pieceAt(x, y);
            if (piece == PieceType.ABSORBER) {
                return new TraceResult(null, 0, bounces);
            }
            if (piece == PieceType.FORWARD || piece == PieceType.BACK) {
                dir = reflect(piece, dir);
                bounces++;
            } else if (piece != null) {
                color |= piece.colorMask();
            }

            int nx = x + dir.deltaX();
            int ny = y + dir.deltaY();
            if (nx < 0 || ny < 0 || nx >= n || ny >= n) {
                return new TraceResult(exitPort(x, y, dir, n), color, bounces);
            }
            x = nx;
            y = ny;
        }
    }

    /** Mirror reflection table (Appendix A). */
    private static Direction reflect(PieceType piece, Direction incoming) {
        if (piece == PieceType.FORWARD) {
            // `/`: Right↔Up, Left↔Down
            switch (incoming) {
                case RIGHT:
                    return Direction.UP;
                case UP:
                    return Direction.RIGHT;
                case LEFT:
                    return Direction.DOWN;
                case DOWN:
                    return Direction.LEFT;
                default:
                    throw new IllegalStateException("unknown direction " + incoming);
            }
        }
        // BACK `\`: Right↔Down, Left↔Up
        switch (incoming) {
            case RIGHT:
                return Direction.DOWN;
            case DOWN:
                return Direction.RIGHT;
            case LEFT:
                return Direction.UP;
            case UP:
                return Direction.LEFT;
            default:
                throw new IllegalStateException("unknown direction " + incoming);
        }
    }

    /** Inverse of entry rules: border port for the edge crossed when leaving {@code (x,y)}. */
    private static Port exitPort(int x, int y, Direction dir, int boardSize) {
        switch (dir) {
            case UP:
                return Port.of(Port.Side.T, x, boardSize);
            case DOWN:
                return Port.of(Port.Side.B, x, boardSize);
            case LEFT:
                return Port.of(Port.Side.L, y, boardSize);
            case RIGHT:
                return Port.of(Port.Side.R, y, boardSize);
            default:
                throw new IllegalStateException("unknown direction " + dir);
        }
    }

    /** Pack (x, y, dir ordinal, colorMask) into a long for the visited set. */
    private static long visitKey(int x, int y, Direction dir, int color) {
        return (((long) color) << 24)
                | (((long) dir.ordinal()) << 16)
                | (((long) y) << 8)
                | ((long) x & 0xffL);
    }
}
