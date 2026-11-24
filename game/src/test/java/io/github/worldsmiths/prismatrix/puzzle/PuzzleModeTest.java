package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PuzzleModeTest {

    @Test
    void fixedPiecesSeededAndBudgetExempt() {
        LevelDefinition level =
                new LevelDefinition(
                        "t-fixed",
                        3,
                        2,
                        1,
                        0,
                        0,
                        OrbColorPalette.Id.RED_YELLOW_BLUE,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 1, 3),
                                        Port.of(Port.Side.L, 1, 3),
                                        0)),
                        Collections.singletonList(
                                new FixedPieceDefinition(1, 1, PieceType.FORWARD)));

        PuzzleMode mode = PuzzleMode.open(level);

        assertTrue(mode.isFixed(1, 1));
        assertEquals(PieceType.FORWARD, mode.pieceAt(1, 1));
        assertEquals(2, mode.remaining());
        assertEquals(2, mode.remainingMirrors());
        assertEquals(1, mode.remainingOrb(PieceType.ORB_1));
        assertEquals(PieceType.FORWARD, mode.mergedBoard().pieceAt(1, 1));
    }

    @Test
    void placeAndRemoveOnFixedCellRefused() {
        LevelDefinition level =
                new LevelDefinition(
                        "t-refuse-fixed",
                        3,
                        1,
                        0,
                        0,
                        0,
                        null,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 1, 3),
                                        Port.of(Port.Side.B, 1, 3),
                                        0)),
                        Collections.singletonList(
                                new FixedPieceDefinition(0, 0, PieceType.BACK)));

        PuzzleMode mode = PuzzleMode.open(level);

        assertEquals(PlaceResult.OCCUPIED, mode.place(0, 0, PieceType.FORWARD));
        assertFalse(mode.remove(0, 0));
        assertEquals(1, mode.remaining());
        assertEquals(PieceType.BACK, mode.pieceAt(0, 0));
    }

    @Test
    void placingOnFreeCellsSpendsPlayerBudget() {
        LevelDefinition level =
                new LevelDefinition(
                        "t-spend",
                        3,
                        1,
                        1,
                        0,
                        0,
                        OrbColorPalette.Id.RED_GREEN_BLUE,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 0, 3),
                                        Port.of(Port.Side.B, 0, 3),
                                        0)),
                        Collections.singletonList(
                                new FixedPieceDefinition(2, 2, PieceType.FORWARD)));

        PuzzleMode mode = PuzzleMode.open(level);
        assertEquals(PlaceResult.OK, mode.place(0, 1, PieceType.BACK));
        assertEquals(PlaceResult.OK, mode.place(1, 1, PieceType.ORB_1));
        assertEquals(0, mode.remainingMirrors());
        assertEquals(0, mode.remainingOrb(PieceType.ORB_1));
        assertEquals(PieceType.FORWARD, mode.mergedBoard().pieceAt(2, 2));
        assertEquals(PieceType.BACK, mode.mergedBoard().pieceAt(0, 1));
        assertEquals(PieceType.ORB_1, mode.mergedBoard().pieceAt(1, 1));
    }

    @Test
    void verifyRefusedWhileMirrorOrOrbRemaining() {
        LevelDefinition level =
                new LevelDefinition(
                        "t-refuse",
                        3,
                        1,
                        1,
                        0,
                        0,
                        OrbColorPalette.Id.RED_YELLOW_BLUE,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 1, 3),
                                        Port.of(Port.Side.B, 1, 3),
                                        0)),
                        Collections.<FixedPieceDefinition>emptyList());

        PuzzleMode mode = PuzzleMode.open(level);
        assertFalse(mode.canVerify());
        assertTrue(mode.verify().isRefused());

        assertEquals(PlaceResult.OK, mode.place(0, 0, PieceType.FORWARD));
        assertFalse(mode.canVerify());
        assertTrue(mode.verify().isRefused());

        assertEquals(PlaceResult.OK, mode.place(1, 0, PieceType.ORB_1));
        assertTrue(mode.canVerify());
        assertFalse(mode.verify().isRefused());
    }

    @Test
    void solvedBoardVerifiesOk() {
        // `/` at (1,1): T1 Down → Left → L1
        LevelDefinition level =
                new LevelDefinition(
                        "t-solved",
                        3,
                        1,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 1, 3),
                                        Port.of(Port.Side.L, 1, 3),
                                        0)));
        PuzzleMode mode = PuzzleMode.open(level);
        assertEquals(PlaceResult.OK, mode.place(1, 1, PieceType.FORWARD));
        assertTrue(mode.canVerify());

        VerifyResult result = mode.verify();

        assertFalse(result.isRefused());
        assertTrue(result.isOk());
        assertTrue(result.mismatchedEntryPorts().isEmpty());
    }

    @Test
    void verifyUsesMergedBoardIncludingFixed() {
        // Fixed `/` at (1,1) solves T1 → L1; player budget 0.
        LevelDefinition level =
                new LevelDefinition(
                        "t-fixed-solve",
                        3,
                        0,
                        0,
                        0,
                        0,
                        null,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 1, 3),
                                        Port.of(Port.Side.L, 1, 3),
                                        0)),
                        Collections.singletonList(
                                new FixedPieceDefinition(1, 1, PieceType.FORWARD)));

        PuzzleMode mode = PuzzleMode.open(level);
        assertTrue(mode.canVerify());
        assertTrue(mode.verify().isOk());
    }

    @Test
    void wrongPlacementFailsWithExactMismatchedEntryPorts() {
        Port entryA = Port.of(Port.Side.T, 1, 3);
        Port entryB = Port.of(Port.Side.T, 0, 3);
        LevelDefinition level =
                new LevelDefinition(
                        "t-wrong",
                        3,
                        1,
                        Arrays.asList(
                                new GoalDefinition(entryA, Port.of(Port.Side.L, 1, 3), 0),
                                new GoalDefinition(entryB, Port.of(Port.Side.B, 0, 3), 0)));
        PuzzleMode mode = PuzzleMode.open(level);
        // `\` at (1,1): T1 → R1 (not L1); T0 still straight → B0 (matches)
        assertEquals(PlaceResult.OK, mode.place(1, 1, PieceType.BACK));

        VerifyResult result = mode.verify();

        assertFalse(result.isRefused());
        assertFalse(result.isOk());
        assertEquals(Collections.singletonList(entryA), result.mismatchedEntryPorts());
        Set<Port> membership = new HashSet<Port>(result.mismatchedEntryPorts());
        assertEquals(Collections.singleton(entryA), membership);
        assertFalse(membership.contains(entryB));
    }

    @Test
    void goalEqualityRequiresSingularExitPortAndColorMask() {
        Port entry = Port.of(Port.Side.T, 1, 3);
        LevelDefinition level =
                new LevelDefinition(
                        "t-color",
                        3,
                        0,
                        Collections.singletonList(
                                new GoalDefinition(entry, Port.of(Port.Side.B, 1, 3), 1)));
        PuzzleMode mode = PuzzleMode.open(level);
        assertTrue(mode.canVerify());

        VerifyResult colorFail = mode.verify();
        assertFalse(colorFail.isOk());
        assertEquals(Collections.singletonList(entry), colorFail.mismatchedEntryPorts());

        LevelDefinition matching =
                new LevelDefinition(
                        "t-color-ok",
                        3,
                        0,
                        Collections.singletonList(
                                new GoalDefinition(entry, Port.of(Port.Side.B, 1, 3), 0)));
        VerifyResult ok = PuzzleMode.open(matching).verify();
        assertTrue(ok.isOk());
    }

    @Test
    void playerBoardExposesPlacementBoard() {
        LevelDefinition level =
                new LevelDefinition(
                        "t-bridge",
                        3,
                        1,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 0, 3),
                                        Port.of(Port.Side.B, 0, 3),
                                        0)));
        PuzzleMode mode = PuzzleMode.open(level);
        assertNull(mode.playerBoard().pieceAt(0, 0));
        assertEquals(1, mode.playerBoard().remainingMirrors());
    }
}
