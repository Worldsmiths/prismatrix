package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;

/**
 * Mutable N×N draft with an untyped shared mirror budget for FORWARD and BACK.
 */
public final class DraftBoard {

    private final int gridSize;
    private final int mirrorBudget;
    private final PieceType[][] cells;
    private int remaining;

    public DraftBoard(int gridSize, int mirrorBudget) {
        if (gridSize < 1) {
            throw new IllegalArgumentException("gridSize must be >= 1, got " + gridSize);
        }
        if (mirrorBudget < 0) {
            throw new IllegalArgumentException("mirrorBudget must be >= 0, got " + mirrorBudget);
        }
        this.gridSize = gridSize;
        this.mirrorBudget = mirrorBudget;
        this.remaining = mirrorBudget;
        this.cells = new PieceType[gridSize][gridSize];
    }

    public PlaceResult place(int x, int y, PieceType type) {
        if (type != PieceType.FORWARD && type != PieceType.BACK) {
            return PlaceResult.INVALID_TYPE;
        }
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return PlaceResult.OUT_OF_BOUNDS;
        }
        if (cells[y][x] != null) {
            return PlaceResult.OCCUPIED;
        }
        if (remaining == 0) {
            return PlaceResult.OVER_BUDGET;
        }
        cells[y][x] = type;
        remaining--;
        return PlaceResult.OK;
    }

    /**
     * Removes a mirror from a filled in-bounds cell and returns it to inventory.
     *
     * @return {@code true} if a mirror was removed; {@code false} if the cell was empty or OOB
     */
    public boolean remove(int x, int y) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return false;
        }
        if (cells[y][x] == null) {
            return false;
        }
        cells[y][x] = null;
        remaining++;
        return true;
    }

    public int remaining() {
        return remaining;
    }

    public int mirrorBudget() {
        return mirrorBudget;
    }

    public int gridSize() {
        return gridSize;
    }

    public BoardState toBoardState() {
        BoardState.Builder builder = BoardState.builder(gridSize);
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                PieceType piece = cells[y][x];
                if (piece != null) {
                    builder.place(x, y, piece);
                }
            }
        }
        return builder.build();
    }
}
