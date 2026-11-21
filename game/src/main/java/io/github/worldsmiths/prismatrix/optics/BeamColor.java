package io.github.worldsmiths.prismatrix.optics;

/**
 * Human-readable Trace color masks ({@link PieceType#COLOR_R}/{@code G}/{@code B} bits). Mask
 * {@code 0} is white (no orb yet / uncolored ray).
 */
public final class BeamColor {

    private BeamColor() {}

    /** RGBA diffuse/emissive for mask bits; {@code 0} → white. */
    public static float[] rgba(int colorMask) {
        int m = colorMask & (PieceType.COLOR_R | PieceType.COLOR_G | PieceType.COLOR_B);
        if (m == 0) {
            return new float[] {1f, 1f, 1f, 1f};
        }
        float r = (m & PieceType.COLOR_R) != 0 ? 1f : 0.12f;
        float g = (m & PieceType.COLOR_G) != 0 ? 1f : 0.12f;
        float b = (m & PieceType.COLOR_B) != 0 ? 1f : 0.12f;
        return new float[] {r, g, b, 1f};
    }

    /** Short label for HUD: {@code white}, {@code red}, {@code red+green}, … */
    public static String label(int colorMask) {
        int m = colorMask & (PieceType.COLOR_R | PieceType.COLOR_G | PieceType.COLOR_B);
        if (m == 0) {
            return "white";
        }
        StringBuilder sb = new StringBuilder();
        if ((m & PieceType.COLOR_R) != 0) {
            sb.append("red");
        }
        if ((m & PieceType.COLOR_G) != 0) {
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append("green");
        }
        if ((m & PieceType.COLOR_B) != 0) {
            if (sb.length() > 0) {
                sb.append('+');
            }
            sb.append("blue");
        }
        return sb.toString();
    }
}
