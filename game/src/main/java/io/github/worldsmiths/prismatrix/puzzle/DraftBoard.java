package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;

/**
 * Puzzle W1 draft: thin adapter over {@link PlacementBoard} with a mirrors-only budget.
 * Places FORWARD/BACK only; Classic orb inventory is unused on this path.
 */
public final class DraftBoard {

    private final PlacementBoard placement;

    public DraftBoard(int gridSize, int mirrorBudget) {
        this.placement = PlacementBoard.mirrorsOnly(gridSize, mirrorBudget);
    }

    public PlaceResult place(int x, int y, PieceType type) {
        return placement.place(x, y, type);
    }

    /**
     * Removes a mirror from a filled in-bounds cell and returns it to inventory.
     *
     * @return {@code true} if a mirror was removed; {@code false} if the cell was empty or OOB
     */
    public boolean remove(int x, int y) {
        return placement.remove(x, y);
    }

    public int remaining() {
        return placement.remainingMirrors();
    }

    public int mirrorBudget() {
        return placement.mirrorBudget();
    }

    public int gridSize() {
        return placement.gridSize();
    }

    /**
     * Piece at cell, or {@code null} if empty / out of bounds.
     */
    public PieceType pieceAt(int x, int y) {
        return placement.pieceAt(x, y);
    }

    public BoardState toBoardState() {
        return placement.toBoardState();
    }
}
