package io.github.worldsmiths.prismatrix.sandbox;

import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ghost-only consistency: Neutral if path ∩ ghost cells is empty; else Green if exit+color match
 * the observation, Red otherwise. Also exposes ghost-only Test Fire Trace.
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

    /**
     * Ghost-only Test Fire: Trace on the ghost board from {@code port}, returning the full
     * {@link TraceResult} (including visited cells) for beams.
     */
    public static TraceResult testFire(GhostBoard ghosts, Port port) {
        if (ghosts == null) {
            throw new IllegalArgumentException("ghosts must not be null");
        }
        if (port == null) {
            throw new IllegalArgumentException("port must not be null");
        }
        return TraceCore.trace(ghosts.toBoardState(), port);
    }

    private static ConsistencyBadge.Kind badgeFor(
            ObservedShot shot, TraceResult traced, Set<Cell> ghostCells) {
        if (!intersects(traced.visitedCells(), ghostCells)) {
            return ConsistencyBadge.Kind.NEUTRAL;
        }
        Port exit = traced.exitPort();
        if (exit != null
                && exit.equals(shot.exitPort())
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
