package io.github.worldsmiths.prismatrix.optics;

/**
 * Cardinal travel directions. Grid {@code +Y} increases Down.
 */
public enum Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    public int deltaX() {
        switch (this) {
            case LEFT:
                return -1;
            case RIGHT:
                return 1;
            default:
                return 0;
        }
    }

    public int deltaY() {
        switch (this) {
            case UP:
                return -1;
            case DOWN:
                return 1;
            default:
                return 0;
        }
    }
}
