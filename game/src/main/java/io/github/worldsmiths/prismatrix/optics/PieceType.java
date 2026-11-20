package io.github.worldsmiths.prismatrix.optics;

/**
 * M1 piece set: mirrors {@code /} (FORWARD) and {@code \} (BACK), RGB orbs, absorber.
 * Splitter and Filter are intentionally absent.
 */
public enum PieceType {
    /** Mirror {@code /}. */
    FORWARD,
    /** Mirror {@code \}. */
    BACK,
    ORB_R,
    ORB_G,
    ORB_B,
    ABSORBER;

    /** Color mask bits: R=1, G=2, B=4. */
    public static final int COLOR_R = 1;
    public static final int COLOR_G = 2;
    public static final int COLOR_B = 4;

    public int colorMask() {
        switch (this) {
            case ORB_R:
                return COLOR_R;
            case ORB_G:
                return COLOR_G;
            case ORB_B:
                return COLOR_B;
            default:
                return 0;
        }
    }
}
