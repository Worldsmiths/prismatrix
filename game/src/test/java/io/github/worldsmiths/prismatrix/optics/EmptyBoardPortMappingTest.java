package io.github.worldsmiths.prismatrix.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Empty-board port axis oracle (HLD Appendix A). Full Trace goldens land in T2;
 * T1 documents entry cell/dir and opposite-border exit mapping.
 */
class EmptyBoardPortMappingTest {

    private static final int N = 3;

    @Test
    void topPortT1EntersCell1_0TravelingDown() {
        Port entry = Port.of(Port.Side.T, 1, N);

        assertEquals(1, entry.entryX(N));
        assertEquals(0, entry.entryY(N));
        assertEquals(Direction.DOWN, entry.entryDirection());
        // Empty 3×3: T1 travels straight → exits B1
        assertEquals(Port.of(Port.Side.B, 1, N), entry.opposite());
    }

    @Test
    void bottomPortB0EntersCell0_2TravelingUp() {
        Port entry = Port.of(Port.Side.B, 0, N);

        assertEquals(0, entry.entryX(N));
        assertEquals(2, entry.entryY(N));
        assertEquals(Direction.UP, entry.entryDirection());
        assertEquals(Port.of(Port.Side.T, 0, N), entry.opposite());
    }

    @Test
    void leftPortL2EntersCell0_2TravelingRight() {
        Port entry = Port.of(Port.Side.L, 2, N);

        assertEquals(0, entry.entryX(N));
        assertEquals(2, entry.entryY(N));
        assertEquals(Direction.RIGHT, entry.entryDirection());
        assertEquals(Port.of(Port.Side.R, 2, N), entry.opposite());
    }

    @Test
    void rightPortR1EntersCell2_1TravelingLeft() {
        Port entry = Port.of(Port.Side.R, 1, N);

        assertEquals(2, entry.entryX(N));
        assertEquals(1, entry.entryY(N));
        assertEquals(Direction.LEFT, entry.entryDirection());
        assertEquals(Port.of(Port.Side.L, 1, N), entry.opposite());
    }
}
