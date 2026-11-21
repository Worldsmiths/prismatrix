package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import org.junit.jupiter.api.Test;

class DraftBoardTest {

    @Test
    void placeWithinBudgetSucceedsAndDecreasesRemaining() {
        DraftBoard board = new DraftBoard(6, 3);

        PlaceResult result = board.place(1, 2, PieceType.FORWARD);

        assertEquals(PlaceResult.OK, result);
        assertEquals(2, board.remaining());
        assertEquals(3, board.mirrorBudget());
    }

    @Test
    void rejectsStackOnOccupiedCell() {
        DraftBoard board = new DraftBoard(4, 2);
        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.FORWARD));

        PlaceResult result = board.place(0, 0, PieceType.BACK);

        assertEquals(PlaceResult.OCCUPIED, result);
        assertEquals(1, board.remaining());
    }

    @Test
    void rejectsOutOfBoundsPlace() {
        DraftBoard board = new DraftBoard(3, 2);

        assertEquals(PlaceResult.OUT_OF_BOUNDS, board.place(-1, 0, PieceType.FORWARD));
        assertEquals(PlaceResult.OUT_OF_BOUNDS, board.place(0, 3, PieceType.BACK));
        assertEquals(2, board.remaining());
    }

    @Test
    void rejectsOverBudgetWhenRemainingIsZero() {
        DraftBoard board = new DraftBoard(3, 1);
        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.FORWARD));

        PlaceResult result = board.place(1, 1, PieceType.BACK);

        assertEquals(PlaceResult.OVER_BUDGET, result);
        assertEquals(0, board.remaining());
    }

    @Test
    void anyMixOfForwardAndBackCountsAgainstSameBudget() {
        DraftBoard board = new DraftBoard(4, 2);

        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.FORWARD));
        assertEquals(PlaceResult.OK, board.place(1, 0, PieceType.BACK));
        assertEquals(0, board.remaining());
        assertEquals(PlaceResult.OVER_BUDGET, board.place(2, 0, PieceType.FORWARD));
    }

    @Test
    void removeRestoresBudget() {
        DraftBoard board = new DraftBoard(4, 2);
        assertEquals(PlaceResult.OK, board.place(2, 1, PieceType.BACK));
        assertEquals(1, board.remaining());

        assertTrue(board.remove(2, 1));

        assertEquals(2, board.remaining());
        assertEquals(PlaceResult.OK, board.place(2, 1, PieceType.FORWARD));
    }

    @Test
    void removeOnEmptyOrOutOfBoundsDoesNotChangeBudget() {
        DraftBoard board = new DraftBoard(3, 1);

        assertFalse(board.remove(0, 0));
        assertFalse(board.remove(-1, 0));
        assertEquals(1, board.remaining());
    }

    @Test
    void toBoardStateReflectsPlacements() {
        DraftBoard board = new DraftBoard(3, 2);
        board.place(0, 1, PieceType.FORWARD);
        board.place(2, 2, PieceType.BACK);

        BoardState snapshot = board.toBoardState();

        assertEquals(3, snapshot.size());
        assertEquals(PieceType.FORWARD, snapshot.pieceAt(0, 1));
        assertEquals(PieceType.BACK, snapshot.pieceAt(2, 2));
        assertEquals(null, snapshot.pieceAt(0, 0));
    }
}
