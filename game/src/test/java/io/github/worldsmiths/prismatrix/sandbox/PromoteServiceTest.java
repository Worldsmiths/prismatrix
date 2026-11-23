package io.github.worldsmiths.prismatrix.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.puzzle.GoalDefinition;
import io.github.worldsmiths.prismatrix.puzzle.PlacementBoard;
import io.github.worldsmiths.prismatrix.puzzle.LevelDefinition;
import io.github.worldsmiths.prismatrix.puzzle.PlaceResult;
import io.github.worldsmiths.prismatrix.puzzle.PuzzleMode;
import io.github.worldsmiths.prismatrix.puzzle.VerifyResult;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class PromoteServiceTest {

    @Test
    void overBudgetLeavesDraftUnchanged() {
        PlacementBoard draft = PlacementBoard.mirrorsOnly(4, 1);
        GhostBoard ghosts = new GhostBoard(draft);
        assertEquals(PlaceResult.OK, ghosts.place(0, 0, PieceType.FORWARD));
        assertEquals(PlaceResult.OK, ghosts.place(1, 0, PieceType.BACK));

        PromoteResult result = PromoteService.promote(ghosts, draft);

        assertEquals(PromoteResult.Kind.OVER_BUDGET, result.kind());
        assertEquals(2, result.need());
        assertEquals(1, result.remaining());
        assertEquals(1, draft.remainingMirrors());
        assertNull(draft.pieceAt(0, 0));
        assertNull(draft.pieceAt(1, 0));
        assertEquals(PieceType.FORWARD, ghosts.pieceAt(0, 0));
        assertEquals(PieceType.BACK, ghosts.pieceAt(1, 0));
    }

    @Test
    void promoteOkThenVerifyOkOnDraft() {
        // `/` at (1,1): T1 Down → Left → L1
        LevelDefinition level =
                new LevelDefinition(
                        "t-promote-verify",
                        3,
                        1,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 1, 3),
                                        Port.of(Port.Side.L, 1, 3),
                                        0)));
        PuzzleMode mode = PuzzleMode.open(level);
        GhostBoard ghosts = new GhostBoard(mode.playerBoard());
        assertEquals(PlaceResult.OK, ghosts.place(1, 1, PieceType.FORWARD));

        PromoteResult promoted = PromoteService.promote(ghosts, mode.playerBoard());

        assertEquals(PromoteResult.Kind.OK, promoted.kind());
        assertEquals(PieceType.FORWARD, mode.pieceAt(1, 1));
        assertEquals(0, mode.remaining());
        assertEquals(PieceType.FORWARD, ghosts.pieceAt(1, 1));

        VerifyResult verified = mode.verify();
        assertTrue(verified.isOk());
    }

    @Test
    void ghostOnlyEditsDoNotChangeVerifyOutcome() {
        LevelDefinition level =
                new LevelDefinition(
                        "t-ghost-only-verify",
                        3,
                        1,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 1, 3),
                                        Port.of(Port.Side.L, 1, 3),
                                        0)));
        PuzzleMode mode = PuzzleMode.open(level);
        assertEquals(PlaceResult.OK, mode.place(1, 1, PieceType.FORWARD));
        assertTrue(mode.verify().isOk());

        GhostBoard ghosts = new GhostBoard(mode.playerBoard());
        assertEquals(PlaceResult.OK, ghosts.place(0, 0, PieceType.BACK));
        assertEquals(PlaceResult.OK, ghosts.place(2, 2, PieceType.FORWARD));

        VerifyResult afterGhosts = mode.verify();
        assertTrue(afterGhosts.isOk());
        assertEquals(PieceType.FORWARD, mode.pieceAt(1, 1));
        assertNull(mode.pieceAt(0, 0));
        assertNull(mode.pieceAt(2, 2));
    }

    @Test
    void cellConflictLeavesDraftUnchanged() {
        PlacementBoard draft = PlacementBoard.mirrorsOnly(3, 2);
        GhostBoard ghosts = new GhostBoard(draft);
        assertEquals(PlaceResult.OK, ghosts.place(1, 1, PieceType.FORWARD));
        // Draft places after ghost was staged — occupancy conflict at promote time
        assertEquals(PlaceResult.OK, draft.place(1, 1, PieceType.BACK));

        PromoteResult result = PromoteService.promote(ghosts, draft);

        assertEquals(PromoteResult.Kind.CELL_CONFLICT, result.kind());
        assertEquals(Collections.singletonList(new Cell(1, 1)), result.conflictCells());
        assertEquals(PieceType.BACK, draft.pieceAt(1, 1));
        assertEquals(1, draft.remainingMirrors());
        assertEquals(PieceType.FORWARD, ghosts.pieceAt(1, 1));
    }
}
