package io.github.worldsmiths.prismatrix.puzzle;

/** Outcome of attempting to place a mirror on the draft board. */
public enum PlaceResult {
    OK,
    OCCUPIED,
    OUT_OF_BOUNDS,
    OVER_BUDGET,
    INVALID_TYPE
}
