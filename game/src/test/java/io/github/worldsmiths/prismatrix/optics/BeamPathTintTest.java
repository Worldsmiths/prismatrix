package io.github.worldsmiths.prismatrix.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BeamPathTintTest {

    @Test
    void redThenYellow_outboundAfterYellowIsOrange() {
        BoardState board =
                BoardState.builder(3)
                        .place(1, 0, PieceType.ORB_1)
                        .place(1, 1, PieceType.ORB_2)
                        .build();
        Port entry = Port.of(Port.Side.T, 1, 3);
        TraceResult traced = TraceCore.trace(board, entry);

        List<Integer> masks =
                BeamPathTint.segmentMasks(
                        board, entry, traced.visitedCells(), traced.exitPort());

        int redYellow = PieceType.COLOR_1 | PieceType.COLOR_2;
        // entry→red: white; red→yellow: red; yellow→exit: orange
        assertEquals(0, masks.get(0).intValue());
        assertEquals(PieceType.COLOR_1, masks.get(1).intValue());
        assertEquals(redYellow, masks.get(masks.size() - 1).intValue());
        assertEquals(redYellow, traced.colorMask());
        assertEquals("orange", OrbColorPalette.redYellowBlue().label(redYellow));
    }

    @Test
    void yellowThenRed_outboundAfterRedIsOrange() {
        BoardState board =
                BoardState.builder(3)
                        .place(1, 0, PieceType.ORB_2)
                        .place(1, 1, PieceType.ORB_1)
                        .build();
        Port entry = Port.of(Port.Side.T, 1, 3);
        TraceResult traced = TraceCore.trace(board, entry);

        List<Integer> masks =
                BeamPathTint.segmentMasks(
                        board, entry, traced.visitedCells(), traced.exitPort());

        int redYellow = PieceType.COLOR_1 | PieceType.COLOR_2;
        assertEquals(0, masks.get(0).intValue());
        assertEquals(PieceType.COLOR_2, masks.get(1).intValue());
        assertEquals(redYellow, masks.get(masks.size() - 1).intValue());
        assertEquals(redYellow, traced.colorMask());
    }

    @Test
    void orangeRgb_distinctFromYellowPrimary() {
        OrbColorPalette ryb = OrbColorPalette.redYellowBlue();
        float[] yellow = ryb.rgba(PieceType.COLOR_2);
        float[] orange = ryb.rgba(PieceType.COLOR_1 | PieceType.COLOR_2);
        assertEquals(1f, yellow[1], 1e-5f);
        // RYB orange #FF6600 — clearly below lemon yellow on the green channel
        assertEquals(0x66 / 255f, orange[1], 1e-5f);
        assertTrue(orange[1] < yellow[1] - 0.4f);
    }
}
