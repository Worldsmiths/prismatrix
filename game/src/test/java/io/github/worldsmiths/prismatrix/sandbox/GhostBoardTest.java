package io.github.worldsmiths.prismatrix.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.puzzle.DraftBoard;
import io.github.worldsmiths.prismatrix.puzzle.PlaceResult;
import org.junit.jupiter.api.Test;

class GhostBoardTest {

    @Test
    void refusesGhostOnDraftOccupiedCell() {
        DraftBoard draft = new DraftBoard(3, 2);
        assertEquals(PlaceResult.OK, draft.place(1, 1, PieceType.FORWARD));

        GhostBoard ghosts = new GhostBoard(draft);

        PlaceResult result = ghosts.place(1, 1, PieceType.BACK);

        assertEquals(PlaceResult.OCCUPIED, result);
        assertNull(ghosts.pieceAt(1, 1));
        assertEquals(PieceType.FORWARD, draft.pieceAt(1, 1));
    }

    @Test
    void allowsUnlimitedGhostsWhileDraftBudgetIsN() {
        DraftBoard draft = new DraftBoard(4, 1);
        GhostBoard ghosts = new GhostBoard(draft);

        assertEquals(PlaceResult.OK, ghosts.place(0, 0, PieceType.FORWARD));
        assertEquals(PlaceResult.OK, ghosts.place(1, 0, PieceType.BACK));
        assertEquals(PlaceResult.OK, ghosts.place(2, 0, PieceType.FORWARD));

        assertEquals(3, ghosts.ghostCount());
        assertEquals(1, draft.remaining());
        assertNull(draft.pieceAt(0, 0));
        assertNull(draft.pieceAt(1, 0));
        assertNull(draft.pieceAt(2, 0));
    }

    @Test
    void refusesOutOfBoundsGhostPlace() {
        DraftBoard draft = new DraftBoard(3, 1);
        GhostBoard ghosts = new GhostBoard(draft);

        assertEquals(PlaceResult.OUT_OF_BOUNDS, ghosts.place(-1, 0, PieceType.FORWARD));
        assertEquals(PlaceResult.OUT_OF_BOUNDS, ghosts.place(0, 3, PieceType.BACK));
        assertEquals(0, ghosts.ghostCount());
    }
}
