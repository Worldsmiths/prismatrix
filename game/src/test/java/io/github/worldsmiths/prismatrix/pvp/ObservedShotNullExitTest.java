package io.github.worldsmiths.prismatrix.pvp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.worldsmiths.prismatrix.optics.Port;
import org.junit.jupiter.api.Test;

class ObservedShotNullExitTest {

    @Test
    void allowsNullExitPortForAbsorbOrCycle() {
        Port entry = Port.of(Port.Side.T, 1, 3);

        ObservedShot shot =
                assertDoesNotThrow(() -> new ObservedShot(entry, null, 0));

        assertEquals(entry, shot.entryPort());
        assertNull(shot.exitPort());
        assertEquals(0, shot.colorMask());
    }
}
