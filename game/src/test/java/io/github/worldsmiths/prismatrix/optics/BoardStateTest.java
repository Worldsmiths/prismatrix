package io.github.worldsmiths.prismatrix.optics;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BoardStateTest {

    @Test
    void rejectsSecondPieceInSameCell() {
        BoardState.Builder builder = BoardState.builder(3).place(1, 1, PieceType.ABSORBER);

        assertThrows(
                IllegalArgumentException.class,
                () -> builder.place(1, 1, PieceType.ORB_1));
    }
}
