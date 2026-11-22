package io.github.worldsmiths.prismatrix.optics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Progressive beam tint masks along a Trace path.
 *
 * <p>Color is acquired when <em>leaving</em> a cell (same moment {@link TraceCore} has already ORed
 * that cell's orb before stepping onward). The segment into an orb stays the previous color; the
 * segment out of the orb uses the updated mix — so the tint does not flip half a cell early.
 *
 * <p>Presentation maps each mask through {@link OrbColorPalette} (never ad-hoc RGB blending).
 */
public final class BeamPathTint {

    private BeamPathTint() {}

    /**
     * Returns one color mask per polyline segment for {@code [entry?] + cells + [exit?]} points.
     * Empty when there are fewer than 2 path points.
     */
    public static List<Integer> segmentMasks(
            BoardState board, Port entryPort, List<Cell> cells, Port exitPort) {
        List<Cell> path = cells == null ? Collections.<Cell>emptyList() : cells;
        int points = (entryPort != null ? 1 : 0) + path.size() + (exitPort != null ? 1 : 0);
        if (points < 2) {
            return Collections.emptyList();
        }
        int segments = points - 1;
        List<Integer> masks = new ArrayList<Integer>(segments);
        int color = 0;
        int cellOffset = entryPort != null ? 1 : 0;
        for (int i = 0; i < segments; i++) {
            // Segment starts at path cell → include that cell's orb for the outbound leg.
            int startCellIdx = i - cellOffset;
            if (startCellIdx >= 0 && startCellIdx < path.size()) {
                color = applyOrb(color, board, path.get(startCellIdx));
            }
            masks.add(Integer.valueOf(color));
        }
        return masks;
    }

    private static int applyOrb(int color, BoardState board, Cell cell) {
        if (board == null || cell == null) {
            return color;
        }
        PieceType piece = board.pieceAt(cell.x(), cell.y());
        if (piece == null) {
            return color;
        }
        int mask = piece.colorMask();
        if (mask == 0) {
            return color;
        }
        return color | mask;
    }
}
