package io.github.worldsmiths.prismatrix.optics;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PortTest {

    @Test
    void rejectsOutOfBoundsIndex() {
        assertThrows(IllegalArgumentException.class, () -> Port.of(Port.Side.T, 3, 3));
        assertThrows(IllegalArgumentException.class, () -> Port.of(Port.Side.B, -1, 3));
    }

    @Test
    void rejectsNullSide() {
        assertThrows(IllegalArgumentException.class, () -> Port.of(null, 0, 3));
    }
}
