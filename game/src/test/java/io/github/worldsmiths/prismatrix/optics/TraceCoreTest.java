package io.github.worldsmiths.prismatrix.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TraceCoreTest {

    @Test
    void rejectsNullBoard() {
        Port entry = Port.of(Port.Side.T, 0, 3);
        assertThrows(IllegalArgumentException.class, () -> TraceCore.trace(null, entry));
    }

    @Test
    void rejectsNullPort() {
        BoardState board = BoardState.builder(3).build();
        assertThrows(IllegalArgumentException.class, () -> TraceCore.trace(board, null));
    }

    @Test
    void rejectsOutOfBoundsPortBeforeTrace() {
        BoardState board = BoardState.builder(3).build();
        // Constructed against a larger board; invalid for this 3×3.
        Port oob = Port.of(Port.Side.T, 4, 5);
        assertThrows(IllegalArgumentException.class, () -> TraceCore.trace(board, oob));
    }

    @Test
    void idempotent_sameBoardAndPortTwice() {
        BoardState board =
                BoardState.builder(3)
                        .place(1, 1, PieceType.FORWARD)
                        .place(0, 1, PieceType.ORB_1)
                        .build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceResult a = TraceCore.trace(board, entry);
        TraceResult b = TraceCore.trace(board, entry);

        assertEqualExitFields(a, b);
    }

    @Test
    void idempotent_absorberNullExitTwice() {
        BoardState board = BoardState.builder(3).place(1, 0, PieceType.ABSORBER).build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceResult a = TraceCore.trace(board, entry);
        TraceResult b = TraceCore.trace(board, entry);

        assertEqualExitFields(a, b);
        assertNull(a.exitPort());
    }

    @Test
    void doesNotMutateBoardState() {
        BoardState board =
                BoardState.builder(3)
                        .place(1, 1, PieceType.BACK)
                        .place(2, 1, PieceType.ORB_2)
                        .build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceCore.trace(board, entry);

        assertEquals(PieceType.BACK, board.pieceAt(1, 1));
        assertEquals(PieceType.ORB_2, board.pieceAt(2, 1));
        assertNull(board.pieceAt(0, 0));
    }

    private static void assertEqualExitFields(TraceResult a, TraceResult b) {
        if (a.exitPort() == null) {
            assertNull(b.exitPort());
        } else {
            assertNotNull(b.exitPort());
            assertEquals(a.exitPort().side(), b.exitPort().side());
            assertEquals(a.exitPort().index(), b.exitPort().index());
        }
        assertEquals(a.colorMask(), b.colorMask());
        assertEquals(a.totalBounces(), b.totalBounces());
    }
}
