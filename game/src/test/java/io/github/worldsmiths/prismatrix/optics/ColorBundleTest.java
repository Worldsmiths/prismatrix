package io.github.worldsmiths.prismatrix.optics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ColorBundleTest {

    @Test
    void ryb_hasDistinctMixOutputs() {
        ColorBundle b = ColorBundle.redYellowBlue();
        assertEquals("red", b.channelName(0));
        assertEquals("yellow", b.channelName(1));
        assertEquals("blue", b.channelName(2));
        assertEquals(0xFF6600, b.pair01Rgb());
        assertEquals("orange", b.pair01Label());
        assertEquals(0x008000, b.pair12Rgb());
        assertEquals("green", b.pair12Label());
        assertEquals(0x800080, b.pair02Rgb());
        assertEquals("purple", b.pair02Label());
        assertEquals(0x663300, b.fullRgb());
        assertEquals("brown", b.fullLabel());
    }

    @Test
    void rgb_hasLightMixOutputs() {
        ColorBundle b = ColorBundle.redGreenBlue();
        assertEquals(0xFFFF00, b.pair01Rgb());
        assertEquals("yellow", b.pair01Label());
        assertEquals(0x00FFFF, b.pair12Rgb());
        assertEquals("cyan", b.pair12Label());
        assertEquals(0xFF00FF, b.pair02Rgb());
        assertEquals("magenta", b.pair02Label());
        assertEquals(0xFFFFFF, b.fullRgb());
        assertEquals("white", b.fullLabel());
    }

    @Test
    void rejectsIncompleteBundle() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ColorBundle(
                                "x",
                                new String[] {"a", "b"},
                                new int[] {1, 2, 3},
                                0,
                                "p01",
                                0,
                                "p12",
                                0,
                                "p02",
                                0,
                                "full"));
    }
}
