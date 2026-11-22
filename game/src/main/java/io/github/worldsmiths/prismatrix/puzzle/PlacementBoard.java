package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;

/**
 * Mutable N×N placement board with typed orb slots and an untyped mirror budget.
 * Classic inventory is 3 primary orbs + 4 mirrors; Puzzle uses mirrors-only budgets.
 */
public final class PlacementBoard {

    public static final int CLASSIC_MIRROR_BUDGET = 4;

    private final int gridSize;
    private final int mirrorBudget;
    private final boolean orbsEnabled;
    private final PieceType[][] cells;
    private int remainingMirrors;
    /** Remaining count per tray channel 0..2 ({@link PieceType#ORB_1}/{@code ORB_2}/{@code ORB_3}). */
    private final int[] remainingOrbs;

    private PlacementBoard(int gridSize, int mirrorBudget, boolean orbsEnabled) {
        if (gridSize < 1) {
            throw new IllegalArgumentException("gridSize must be >= 1, got " + gridSize);
        }
        if (mirrorBudget < 0) {
            throw new IllegalArgumentException("mirrorBudget must be >= 0, got " + mirrorBudget);
        }
        this.gridSize = gridSize;
        this.mirrorBudget = mirrorBudget;
        this.orbsEnabled = orbsEnabled;
        this.remainingMirrors = mirrorBudget;
        this.remainingOrbs = new int[] {orbsEnabled ? 1 : 0, orbsEnabled ? 1 : 0, orbsEnabled ? 1 : 0};
        this.cells = new PieceType[gridSize][gridSize];
    }

    /** Classic Mode 1 inventory: one of each primary orb + 4 untyped mirror slots. */
    public static PlacementBoard classic(int gridSize) {
        return new PlacementBoard(gridSize, CLASSIC_MIRROR_BUDGET, true);
    }

    /** Mirrors-only inventory for Puzzle W1 budgets (orbs unused / rejected). */
    public static PlacementBoard mirrorsOnly(int gridSize, int mirrorBudget) {
        return new PlacementBoard(gridSize, mirrorBudget, false);
    }

    public PlaceResult place(int x, int y, PieceType type) {
        if (type == null) {
            return PlaceResult.INVALID_TYPE;
        }
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return PlaceResult.OUT_OF_BOUNDS;
        }
        if (cells[y][x] != null) {
            return PlaceResult.OCCUPIED;
        }
        if (type == PieceType.FORWARD || type == PieceType.BACK) {
            if (remainingMirrors == 0) {
                return PlaceResult.OVER_BUDGET;
            }
            cells[y][x] = type;
            remainingMirrors--;
            return PlaceResult.OK;
        }
        if (type.isOrb()) {
            if (!orbsEnabled) {
                return PlaceResult.INVALID_TYPE;
            }
            int remaining = remainingOrb(type);
            if (remaining == 0) {
                return PlaceResult.OVER_BUDGET;
            }
            cells[y][x] = type;
            decrementOrb(type);
            return PlaceResult.OK;
        }
        return PlaceResult.INVALID_TYPE;
    }

    public boolean remove(int x, int y) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return false;
        }
        PieceType existing = cells[y][x];
        if (existing == null) {
            return false;
        }
        cells[y][x] = null;
        if (existing == PieceType.FORWARD || existing == PieceType.BACK) {
            remainingMirrors++;
        } else if (existing.isOrb()) {
            incrementOrb(existing);
        }
        return true;
    }

    private void decrementOrb(PieceType type) {
        remainingOrbs[type.orbChannel()]--;
    }

    private void incrementOrb(PieceType type) {
        remainingOrbs[type.orbChannel()]++;
    }

    public int remainingMirrors() {
        return remainingMirrors;
    }

    public int mirrorBudget() {
        return mirrorBudget;
    }

    public int remainingOrb(PieceType type) {
        if (type == null || !type.isOrb()) {
            throw new IllegalArgumentException("not an orb type: " + type);
        }
        return remainingOrbs[type.orbChannel()];
    }

    public int remainingOrbs() {
        return remainingOrbs[0] + remainingOrbs[1] + remainingOrbs[2];
    }

    public int gridSize() {
        return gridSize;
    }

    public PieceType pieceAt(int x, int y) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return null;
        }
        return cells[y][x];
    }

    /** True when Classic inventory is fully placed (3 orbs + 4 mirrors). */
    public boolean isCompleteClassic() {
        return orbsEnabled
                && remainingOrbs() == 0
                && remainingMirrors == 0
                && mirrorBudget == CLASSIC_MIRROR_BUDGET;
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
