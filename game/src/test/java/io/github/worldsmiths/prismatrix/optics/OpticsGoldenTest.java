package io.github.worldsmiths.prismatrix.optics;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class OpticsGoldenTest {

    @Test
    void empty3x3_T1_exitsB1_white_zeroBounces() {
        BoardState board = BoardState.builder(3).build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceResult result = TraceCore.trace(board, entry);

        assertNotNull(result.exitPort());
        assertEquals(Port.Side.B, result.exitPort().side());
        assertEquals(1, result.exitPort().index());
        assertEquals(0, result.colorMask());
        assertEquals(0, result.totalBounces());
    }

    @Test
    void emptyCorridor_T1_recordsOrderedVisitedCells() {
        BoardState board = BoardState.builder(3).build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceResult result = TraceCore.trace(board, entry);

        List<Cell> path = result.visitedCells();
        assertFalse(path.isEmpty(), "multi-cell ray must record visited cells");
        assertEquals(
                Arrays.asList(new Cell(1, 0), new Cell(1, 1), new Cell(1, 2)), path);
    }

    @Test
    void mirrorCorridor_T1_forward_recordsOrderedVisitedCells() {
        // `/` at (1,1): (1,0) Down → (1,1) reflect Down→Left → (0,1) → exit L1
        BoardState board = BoardState.builder(3).place(1, 1, PieceType.FORWARD).build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceResult result = TraceCore.trace(board, entry);

        List<Cell> path = result.visitedCells();
        assertFalse(path.isEmpty(), "multi-cell ray must record visited cells");
        assertEquals(
                Arrays.asList(new Cell(1, 0), new Cell(1, 1), new Cell(0, 1)), path);
        assertEquals(Port.Side.L, result.exitPort().side());
        assertEquals(1, result.exitPort().index());
    }

    @Test
    void mirrorForward_T1_redirectsToL1_oneBounce() {
        // `/` at (1,1): enter T1 Down → reflect Down→Left → exit L1
        BoardState board = BoardState.builder(3).place(1, 1, PieceType.FORWARD).build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceResult result = TraceCore.trace(board, entry);

        assertNotNull(result.exitPort());
        assertEquals(Port.Side.L, result.exitPort().side());
        assertEquals(1, result.exitPort().index());
        assertEquals(0, result.colorMask());
        assertEquals(1, result.totalBounces());
    }

    @Test
    void mirrorBack_T1_redirectsToR1_oneBounce() {
        // `\` at (1,1): enter T1 Down → reflect Down→Right → exit R1
        BoardState board = BoardState.builder(3).place(1, 1, PieceType.BACK).build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceResult result = TraceCore.trace(board, entry);

        assertNotNull(result.exitPort());
        assertEquals(Port.Side.R, result.exitPort().side());
        assertEquals(1, result.exitPort().index());
        assertEquals(0, result.colorMask());
        assertEquals(1, result.totalBounces());
    }

    @Test
    void orbMix_exitColorMaskIsBitwiseOr() {
        BoardState board =
                BoardState.builder(3)
                        .place(1, 0, PieceType.ORB_R)
                        .place(1, 1, PieceType.ORB_G)
                        .place(1, 2, PieceType.ORB_B)
                        .build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceResult result = TraceCore.trace(board, entry);

        assertNotNull(result.exitPort());
        assertEquals(Port.Side.B, result.exitPort().side());
        assertEquals(1, result.exitPort().index());
        assertEquals(
                PieceType.COLOR_R | PieceType.COLOR_G | PieceType.COLOR_B, result.colorMask());
        assertEquals(0, result.totalBounces());
    }

    @Test
    void absorber_nullExit_colorMaskZero() {
        BoardState board = BoardState.builder(3).place(1, 1, PieceType.ABSORBER).build();
        Port entry = Port.of(Port.Side.T, 1, 3);

        TraceResult result = TraceCore.trace(board, entry);

        assertNull(result.exitPort());
        assertEquals(0, result.colorMask());
        assertEquals(0, result.totalBounces());
    }

    @Test
    void cycle_twoMirrorPairs_nullExitWithoutThrow() {
        // Closed 2×2 mirror loop (two `/` + two `\`). Border ports cannot enter this
        // orbit under Appendix A; start mid-grid on the loop state via package helper.
        //   / \
        //   \ /
        BoardState board =
                BoardState.builder(2)
                        .place(0, 0, PieceType.FORWARD)
                        .place(1, 0, PieceType.BACK)
                        .place(0, 1, PieceType.BACK)
                        .place(1, 1, PieceType.FORWARD)
                        .build();

        TraceResult result =
                assertDoesNotThrow(
                        () -> TraceCore.traceAt(board, 0, 0, Direction.UP, 0));

        assertNull(result.exitPort());
        assertEquals(0, result.colorMask());
    }
}
