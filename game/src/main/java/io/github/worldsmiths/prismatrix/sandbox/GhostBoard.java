package io.github.worldsmiths.prismatrix.sandbox;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.puzzle.PlaceResult;
import io.github.worldsmiths.prismatrix.puzzle.PlacementBoard;

/**
 * Unlimited provisional Ghost layer. Places FORWARD/BACK only on cells that are empty on the
 * referenced player board; never mutates the board.
 */
public final class GhostBoard {

    private final PlacementBoard playerBoard;
    private final int gridSize;
    private final PieceType[][] cells;

    public GhostBoard(PlacementBoard playerBoard) {
        if (playerBoard == null) {
            throw new IllegalArgumentException("playerBoard must not be null");
        }
        this.playerBoard = playerBoard;
        this.gridSize = playerBoard.gridSize();
        this.cells = new PieceType[gridSize][gridSize];
    }

    public PlaceResult place(int x, int y, PieceType type) {
        if (type != PieceType.FORWARD && type != PieceType.BACK) {
            return PlaceResult.INVALID_TYPE;
        }
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return PlaceResult.OUT_OF_BOUNDS;
        }
        if (playerBoard.pieceAt(x, y) != null) {
            return PlaceResult.OCCUPIED;
        }
        if (cells[y][x] != null) {
            return PlaceResult.OCCUPIED;
        }
        cells[y][x] = type;
        return PlaceResult.OK;
    }

    public boolean remove(int x, int y) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return false;
        }
        if (cells[y][x] == null) {
            return false;
        }
        cells[y][x] = null;
        return true;
    }

    public PieceType pieceAt(int x, int y) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) {
            return null;
        }
        return cells[y][x];
    }

    public int gridSize() {
        return gridSize;
    }

    public int ghostCount() {
        int count = 0;
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                if (cells[y][x] != null) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Snapshot of ghost pieces only (no player pieces). Suitable for Ghost-only Trace. */
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
