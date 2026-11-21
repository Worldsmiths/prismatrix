package io.github.worldsmiths.prismatrix.pvp;

/** Dual-seat identifier for a Classic match. */
public enum SeatId {
    A,
    B;

    public SeatId opponent() {
        return this == A ? B : A;
    }
}
