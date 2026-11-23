package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import org.junit.jupiter.api.Test;

class PlacementBoardTest {

    @Test
    void classicPlaceOrbSucceedsAndDecreasesThatOrbRemaining() {
        PlacementBoard board = PlacementBoard.classic(8);

        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.ORB_1));
        assertEquals(0, board.remainingOrb(PieceType.ORB_1));
        assertEquals(1, board.remainingOrb(PieceType.ORB_2));
        assertEquals(1, board.remainingOrb(PieceType.ORB_3));
        assertEquals(PieceType.ORB_1, board.pieceAt(0, 0));
    }

    @Test
    void classicPlacesExactlyThreeOrbsAndFourOrientationChosenMirrors() {
        PlacementBoard board = PlacementBoard.classic(8);

        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.ORB_1));
        assertEquals(PlaceResult.OK, board.place(1, 0, PieceType.ORB_2));
        assertEquals(PlaceResult.OK, board.place(2, 0, PieceType.ORB_3));
        assertEquals(PlaceResult.OK, board.place(0, 1, PieceType.FORWARD));
        assertEquals(PlaceResult.OK, board.place(1, 1, PieceType.BACK));
        assertEquals(PlaceResult.OK, board.place(2, 1, PieceType.FORWARD));
        assertEquals(PlaceResult.OK, board.place(3, 1, PieceType.BACK));

        assertEquals(0, board.remainingOrbs());
        assertEquals(0, board.remainingMirrors());
        assertEquals(4, board.mirrorBudget());
        assertTrue(board.isCompleteClassic());
    }

    @Test
    void refusesOverBudgetOrbOfSameColor() {
        PlacementBoard board = PlacementBoard.classic(8);
        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.ORB_1));

        assertEquals(PlaceResult.OVER_BUDGET, board.place(1, 0, PieceType.ORB_1));
        assertEquals(0, board.remainingOrb(PieceType.ORB_1));
        assertEquals(null, board.pieceAt(1, 0));
    }

    @Test
    void refusesOverBudgetMirrorsBeyondFour() {
        PlacementBoard board = PlacementBoard.classic(8);
        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.FORWARD));
        assertEquals(PlaceResult.OK, board.place(1, 0, PieceType.BACK));
        assertEquals(PlaceResult.OK, board.place(2, 0, PieceType.FORWARD));
        assertEquals(PlaceResult.OK, board.place(3, 0, PieceType.BACK));

        assertEquals(PlaceResult.OVER_BUDGET, board.place(4, 0, PieceType.FORWARD));
        assertEquals(0, board.remainingMirrors());
    }

    @Test
    void rejectsAbsorberAsInvalidType() {
        PlacementBoard board = PlacementBoard.classic(8);

        assertEquals(PlaceResult.INVALID_TYPE, board.place(0, 0, PieceType.ABSORBER));
        assertEquals(3, board.remainingOrbs());
        assertEquals(4, board.remainingMirrors());
    }

    @Test
    void mirrorsOnlyRejectsOrbsAsInvalidType() {
        PlacementBoard board = PlacementBoard.mirrorsOnly(6, 3);

        assertEquals(PlaceResult.INVALID_TYPE, board.place(0, 0, PieceType.ORB_1));
        assertEquals(PlaceResult.OK, board.place(1, 0, PieceType.FORWARD));
        assertEquals(2, board.remainingMirrors());
        assertFalse(board.isCompleteClassic());
    }

    @Test
    void puzzleFactorySpendsConfiguredOrbAndMirrorBudgets() {
        PlacementBoard board = PlacementBoard.puzzle(6, 2, new int[] {1, 0, 2});

        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.ORB_1));
        assertEquals(PlaceResult.OK, board.place(1, 0, PieceType.ORB_3));
        assertEquals(PlaceResult.OK, board.place(2, 0, PieceType.FORWARD));
        assertEquals(0, board.remainingOrb(PieceType.ORB_1));
        assertEquals(0, board.remainingOrb(PieceType.ORB_2));
        assertEquals(1, board.remainingOrb(PieceType.ORB_3));
        assertEquals(1, board.remainingMirrors());
        assertEquals(PlaceResult.OVER_BUDGET, board.place(3, 0, PieceType.ORB_1));
        assertFalse(board.isCompleteClassic());
    }

    @Test
    void removeRestoresOrbAndMirrorInventory() {
        PlacementBoard board = PlacementBoard.classic(8);
        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.ORB_2));
        assertEquals(PlaceResult.OK, board.place(1, 0, PieceType.BACK));

        assertTrue(board.remove(0, 0));
        assertTrue(board.remove(1, 0));

        assertEquals(1, board.remainingOrb(PieceType.ORB_2));
        assertEquals(4, board.remainingMirrors());
        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.ORB_2));
        assertEquals(PlaceResult.OK, board.place(1, 0, PieceType.FORWARD));
    }

    @Test
    void rejectsOccupiedAndOutOfBounds() {
        PlacementBoard board = PlacementBoard.classic(4);
        assertEquals(PlaceResult.OK, board.place(0, 0, PieceType.ORB_1));

        assertEquals(PlaceResult.OCCUPIED, board.place(0, 0, PieceType.FORWARD));
        assertEquals(PlaceResult.OUT_OF_BOUNDS, board.place(-1, 0, PieceType.BACK));
        assertEquals(PlaceResult.OUT_OF_BOUNDS, board.place(0, 4, PieceType.ORB_2));
    }

    @Test
    void toBoardStateReflectsPlacements() {
        PlacementBoard board = PlacementBoard.classic(3);
        board.place(0, 0, PieceType.ORB_1);
        board.place(1, 2, PieceType.FORWARD);

        BoardState snapshot = board.toBoardState();

        assertEquals(3, snapshot.size());
        assertEquals(PieceType.ORB_1, snapshot.pieceAt(0, 0));
        assertEquals(PieceType.FORWARD, snapshot.pieceAt(1, 2));
        assertEquals(null, snapshot.pieceAt(1, 1));
    }
}
