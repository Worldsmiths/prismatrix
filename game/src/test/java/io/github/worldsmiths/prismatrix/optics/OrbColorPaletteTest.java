package io.github.worldsmiths.prismatrix.optics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class OrbColorPaletteTest {

    private static final float EPS = 1e-5f;

    @Test
    void sourceWhite_alwaysWhite() {
        for (OrbColorPalette.Id id : OrbColorPalette.Id.values()) {
            assertRgba(OrbColorPalette.of(id).rgba(0), 0xFFFFFF);
            assertEquals("white", OrbColorPalette.of(id).label(0));
        }
    }

    @Test
    void ryb_trayOrderIsRedYellowBlue_redPlusYellowIsOrange() {
        OrbColorPalette ryb = OrbColorPalette.redYellowBlue();
        assertEquals("red", ryb.label(PieceType.ORB_1.colorMask()));
        assertEquals("yellow", ryb.label(PieceType.ORB_2.colorMask()));
        assertEquals("blue", ryb.label(PieceType.ORB_3.colorMask()));

        int redYellow = PieceType.COLOR_1 | PieceType.COLOR_2;
        assertEquals("orange", ryb.label(redYellow));
        assertRgba(ryb.rgba(redYellow), 0xFF6600);

        assertEquals("green", ryb.label(PieceType.COLOR_2 | PieceType.COLOR_3));
        assertRgba(ryb.rgba(PieceType.COLOR_2 | PieceType.COLOR_3), 0x008000);

        assertEquals("purple", ryb.label(PieceType.COLOR_1 | PieceType.COLOR_3));
        assertRgba(ryb.rgba(PieceType.COLOR_1 | PieceType.COLOR_3), 0x800080);

        assertEquals("brown", ryb.label(OrbColorPalette.FULL_SPECTRUM));
        assertRgba(ryb.rgba(OrbColorPalette.FULL_SPECTRUM), 0x663300);
        assertNotEquals(ryb.rgba(0)[1], ryb.rgba(OrbColorPalette.FULL_SPECTRUM)[1], 0.2f);
    }

    @Test
    void rgb_hexMixTable() {
        OrbColorPalette rgb = OrbColorPalette.redGreenBlue();
        assertEquals("yellow", rgb.label(PieceType.COLOR_1 | PieceType.COLOR_2));
        assertEquals("cyan", rgb.label(PieceType.COLOR_2 | PieceType.COLOR_3));
        assertEquals("magenta", rgb.label(PieceType.COLOR_1 | PieceType.COLOR_3));
        assertEquals("white", rgb.label(OrbColorPalette.FULL_SPECTRUM));

        assertRgba(rgb.rgba(PieceType.COLOR_1 | PieceType.COLOR_2), 0xFFFF00);
        assertRgba(rgb.rgba(PieceType.COLOR_2 | PieceType.COLOR_3), 0x00FFFF);
        assertRgba(rgb.rgba(PieceType.COLOR_1 | PieceType.COLOR_3), 0xFF00FF);
        assertRgba(rgb.rgba(OrbColorPalette.FULL_SPECTRUM), 0xFFFFFF);
    }

    @Test
    void pieceColors_followTrayChannels() {
        OrbColorPalette ryb = OrbColorPalette.redYellowBlue();
        assertRgba(ryb.rgbaForPiece(PieceType.ORB_1), 0xFF0000);
        assertRgba(ryb.rgbaForPiece(PieceType.ORB_2), 0xFFFF00);
        assertRgba(ryb.rgbaForPiece(PieceType.ORB_3), 0x0000FF);
        assertArrayEquals(ryb.channelRgba(1), ryb.rgbaForPiece(PieceType.ORB_2), EPS);
    }

    @Test
    void palette_usesBundleMixTable() {
        ColorBundle bundle = ColorBundle.redYellowBlue();
        OrbColorPalette palette =
                OrbColorPalette.fromBundle(OrbColorPalette.Id.RED_YELLOW_BLUE, bundle);
        assertEquals(bundle.pair01Label(), palette.label(PieceType.COLOR_1 | PieceType.COLOR_2));
        assertRgba(palette.rgba(PieceType.COLOR_1 | PieceType.COLOR_2), bundle.pair01Rgb());
        assertRgba(palette.rgba(OrbColorPalette.FULL_SPECTRUM), bundle.fullRgb());
        assertEquals(bundle, palette.bundle());
    }

    private static void assertRgba(float[] actual, int rgb) {
        float[] expected =
                new float[] {
                    ((rgb >> 16) & 0xff) / 255f,
                    ((rgb >> 8) & 0xff) / 255f,
                    (rgb & 0xff) / 255f,
                    1f
                };
        assertArrayEquals(expected, actual, EPS);
    }
}