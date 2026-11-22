package io.github.worldsmiths.prismatrix.sandbox;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Ghost-only consistency badges: Neutral if path ∩ ghost cells is empty; else Green if exit+color
 * match the observation, Red otherwise. Test Fire traces any {@link BoardState} (Ghost or Draft).
 */
public final class ConsistencyEngine {

    private ConsistencyEngine() {}

    public static List<ConsistencyBadge> evaluate(GhostBoard ghosts, List<ObservedShot> observations) {
        if (ghosts == null) {
            throw new IllegalArgumentException("ghosts must not be null");
        }
        if (observations == null) {
            throw new IllegalArgumentException("observations must not be null");
        }

        Set<Cell> ghostCells = ghostCellSet(ghosts);
        List<ConsistencyBadge> badges = new ArrayList<ConsistencyBadge>(observations.size());
        for (ObservedShot shot : observations) {
            TraceResult traced = TraceCore.trace(ghosts.toBoardState(), shot.entryPort());
            badges.add(new ConsistencyBadge(shot.entryPort(), badgeFor(shot, traced, ghostCells)));
        }
        return badges;
    }

    /** Test Fire on the ghost board. */
    public static TraceResult testFire(GhostBoard ghosts, Port port) {
        if (ghosts == null) {
            throw new IllegalArgumentException("ghosts must not be null");
        }
        return testFire(ghosts.toBoardState(), port);
    }

    /**
     * Test Fire Trace on an arbitrary board (Ghost or Draft snapshot) from {@code port}, returning
     * visited cells for beams.
     */
    public static TraceResult testFire(BoardState board, Port port) {
        if (board == null) {
            throw new IllegalArgumentException("board must not be null");
        }
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }
        return TraceCore.trace(board, port);
    }

    /** Package-visible for null-exit absorb compare coverage. */
    static ConsistencyBadge.Kind badgeFor(
            ObservedShot shot, TraceResult traced, Set<Cell> ghostCells) {
        if (!intersects(traced.visitedCells(), ghostCells)) {
            return ConsistencyBadge.Kind.NEUTRAL;
        }
        // Null exits (absorb/cycle) must compare equal without NPE.
        if (Objects.equals(traced.exitPort(), shot.exitPort())
                && traced.colorMask() == shot.colorMask()) {
            return ConsistencyBadge.Kind.GREEN;
        }
        return ConsistencyBadge.Kind.RED;
    }

    private static boolean intersects(List<Cell> path, Set<Cell> ghostCells) {
        for (Cell cell : path) {
            if (ghostCells.contains(cell)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Cell> ghostCellSet(GhostBoard ghosts) {
        Set<Cell> set = new HashSet<Cell>();
        int n = ghosts.gridSize();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (ghosts.pieceAt(x, y) != null) {
                    set.add(new Cell(x, y));
                }
            }
        }
        return set;
    }
}
