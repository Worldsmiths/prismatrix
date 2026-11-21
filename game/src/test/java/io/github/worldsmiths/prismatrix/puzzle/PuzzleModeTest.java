package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PuzzleModeTest {

    @Test
    void verifyRefusedWhileRemainingBudgetGreaterThanZero() {
        LevelDefinition level =
                new LevelDefinition(
                        "t-refuse",
                        3,
                        1,
                        Collections.singletonList(
                                new ClueDefinition(
                                        Port.of(Port.Side.T, 1, 3),
                                        Port.of(Port.Side.B, 1, 3),
                                        0)));

        PuzzleMode mode = PuzzleMode.open(level);

        assertFalse(mode.canVerify());
        assertEquals(1, mode.remaining());

        VerifyResult result = mode.verify();

        assertTrue(result.isRefused());
        assertFalse(result.isOk());
        assertTrue(result.mismatchedEntryPorts().isEmpty());
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
                                new ClueDefinition(
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
    void wrongPlacementFailsWithExactMismatchedEntryPorts() {
        Port entryA = Port.of(Port.Side.T, 1, 3);
        Port entryB = Port.of(Port.Side.T, 0, 3);
        LevelDefinition level =
                new LevelDefinition(
                        "t-wrong",
                        3,
                        1,
                        Arrays.asList(
                                new ClueDefinition(
                                        entryA, Port.of(Port.Side.L, 1, 3), 0),
                                new ClueDefinition(
                                        entryB, Port.of(Port.Side.B, 0, 3), 0)));
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
    void clueEqualityRequiresSingularExitPortAndColorMask() {
        // Empty board: T1 → B1 with colorMask 0. Clue demands colorMask 1 → FAIL.
        Port entry = Port.of(Port.Side.T, 1, 3);
        LevelDefinition level =
                new LevelDefinition(
                        "t-color",
                        3,
                        0,
                        Collections.singletonList(
                                new ClueDefinition(
                                        entry, Port.of(Port.Side.B, 1, 3), 1)));
        PuzzleMode mode = PuzzleMode.open(level);
        assertTrue(mode.canVerify());

        VerifyResult colorFail = mode.verify();
        assertFalse(colorFail.isOk());
        assertEquals(Collections.singletonList(entry), colorFail.mismatchedEntryPorts());

        // Matching exit + colorMask 0 → OK
        LevelDefinition matching =
                new LevelDefinition(
                        "t-color-ok",
                        3,
                        0,
                        Collections.singletonList(
                                new ClueDefinition(
                                        entry, Port.of(Port.Side.B, 1, 3), 0)));
        VerifyResult ok = PuzzleMode.open(matching).verify();
        assertTrue(ok.isOk());
    }
}
