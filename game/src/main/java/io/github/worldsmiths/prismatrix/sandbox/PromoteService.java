package io.github.worldsmiths.prismatrix.sandbox;

import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.puzzle.DraftBoard;
import io.github.worldsmiths.prismatrix.puzzle.PlaceResult;
import java.util.ArrayList;
import java.util.List;

/**
 * All-or-nothing Ghost → Draft promote. On refuse, draft is unchanged. On OK, ghosts remain.
 */
public final class PromoteService {

    private PromoteService() {}

    public static PromoteResult promote(GhostBoard ghosts, DraftBoard draft) {
        if (ghosts == null) {
            throw new IllegalArgumentException("ghosts must not be null");
        }
        if (draft == null) {
            throw new IllegalArgumentException("draft must not be null");
        }
        if (ghosts.gridSize() != draft.gridSize()) {
            throw new IllegalArgumentException("ghost and draft grid sizes must match");
        }

        int n = ghosts.gridSize();
        List<Cell> placements = new ArrayList<Cell>();
        List<PieceType> types = new ArrayList<PieceType>();
        List<Cell> conflicts = new ArrayList<Cell>();

        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                PieceType ghost = ghosts.pieceAt(x, y);
                if (ghost == null) {
                    continue;
                }
                if (draft.pieceAt(x, y) != null) {
                    conflicts.add(new Cell(x, y));
                } else {
                    placements.add(new Cell(x, y));
                    types.add(ghost);
                }
            }
        }

        if (!conflicts.isEmpty()) {
            return PromoteResult.cellConflict(conflicts);
        }

        int need = placements.size();
        int remaining = draft.remaining();
        if (need > remaining) {
            return PromoteResult.overBudget(need, remaining);
        }

        for (int i = 0; i < placements.size(); i++) {
            Cell cell = placements.get(i);
            PlaceResult placed = draft.place(cell.x(), cell.y(), types.get(i));
            if (placed != PlaceResult.OK) {
                throw new IllegalStateException(
                        "promote place failed after prechecks: " + placed + " at " + cell);
            }
        }
        return PromoteResult.ok();
    }
}
