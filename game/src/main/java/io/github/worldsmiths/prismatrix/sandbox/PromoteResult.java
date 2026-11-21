package io.github.worldsmiths.prismatrix.sandbox;

import io.github.worldsmiths.prismatrix.optics.Cell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outcome of all-or-nothing Ghost → Draft promote. */
public final class PromoteResult {

    public enum Kind {
        OK,
        OVER_BUDGET,
        CELL_CONFLICT
    }

    private final Kind kind;
    private final int need;
    private final int remaining;
    private final List<Cell> conflictCells;

    private PromoteResult(Kind kind, int need, int remaining, List<Cell> conflictCells) {
        this.kind = kind;
        this.need = need;
        this.remaining = remaining;
        this.conflictCells =
                Collections.unmodifiableList(new ArrayList<Cell>(conflictCells));
    }

    public static PromoteResult ok() {
        return new PromoteResult(Kind.OK, 0, 0, Collections.<Cell>emptyList());
    }

    public static PromoteResult overBudget(int need, int remaining) {
        return new PromoteResult(Kind.OVER_BUDGET, need, remaining, Collections.<Cell>emptyList());
    }

    public static PromoteResult cellConflict(List<Cell> cells) {
        if (cells == null) {
            throw new IllegalArgumentException("cells must not be null");
        }
        return new PromoteResult(Kind.CELL_CONFLICT, 0, 0, cells);
    }

    public Kind kind() {
        return kind;
    }

    /** Ghosts that would be promoted (meaningful for OVER_BUDGET). */
    public int need() {
        return need;
    }

    /** Draft remaining at refuse time (meaningful for OVER_BUDGET). */
    public int remaining() {
        return remaining;
    }

    /** Cells that conflicted with draft (meaningful for CELL_CONFLICT). */
    public List<Cell> conflictCells() {
        return conflictCells;
    }
}
