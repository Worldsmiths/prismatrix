package io.github.worldsmiths.prismatrix.optics;

/**
 * Immutable N×N board with at most one piece per cell.
 */
public final class BoardState {

    private final int size;
    private final PieceType[][] cells;

    private BoardState(int size, PieceType[][] cells) {
        this.size = size;
        this.cells = cells;
    }

    public static Builder builder(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("board size must be >= 1, got " + size);
        }
        return new Builder(size);
    }

    public int size() {
        return size;
    }

    public PieceType pieceAt(int x, int y) {
        return cells[y][x];
    }

    public static final class Builder {
        private final int size;
        private final PieceType[][] cells;
        private boolean built;

        private Builder(int size) {
            this.size = size;
            this.cells = new PieceType[size][size];
        }

        public Builder place(int x, int y, PieceType type) {
            ensureMutable();
            validateCell(x, y);
            if (type == null) {
                throw new IllegalArgumentException("piece type must not be null");
            }
            if (cells[y][x] != null) {
                throw new IllegalArgumentException(
                        "cell (" + x + "," + y + ") already has a piece");
            }
            cells[y][x] = type;
            return this;
        }

        public BoardState build() {
            ensureMutable();
            built = true;
            PieceType[][] copy = new PieceType[size][size];
            for (int y = 0; y < size; y++) {
                System.arraycopy(cells[y], 0, copy[y], 0, size);
            }
            return new BoardState(size, copy);
        }

        private void ensureMutable() {
            if (built) {
                throw new IllegalStateException("builder already built");
            }
        }

        private void validateCell(int x, int y) {
            if (x < 0 || x >= size || y < 0 || y >= size) {
                throw new IllegalArgumentException(
                        "cell (" + x + "," + y + ") out of bounds for size " + size);
            }
        }
    }
}
