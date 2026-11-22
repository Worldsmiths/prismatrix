package io.github.worldsmiths.prismatrix.optics;

/**
 * M1 piece set: mirrors {@code /} (FORWARD) and {@code \} (BACK), three primary-channel orbs,
 * absorber. Orb bits {@link #COLOR_1}/{@link #COLOR_2}/{@link #COLOR_3} bind to palette channels
 * 0/1/2 ({@link ColorBundle} / {@link OrbColorPalette}: default red/yellow/blue). Splitter and
 * Filter are intentionally absent.
 */
public enum PieceType {
    /** Mirror {@code /}. */
    FORWARD,
    /** Mirror {@code \}. */
    BACK,
    /** Tray channel 0 orb (bit {@link #COLOR_1}). */
    ORB_1,
    /** Tray channel 1 orb (bit {@link #COLOR_2}). */
    ORB_2,
    /** Tray channel 2 orb (bit {@link #COLOR_3}). */
    ORB_3,
    ABSORBER;

    /** Channel 0 bit. */
    public static final int COLOR_1 = 1;
    /** Channel 1 bit. */
    public static final int COLOR_2 = 2;
    /** Channel 2 bit. */
    public static final int COLOR_3 = 4;

    /** All three channel bits. */
    public static final int ALL_CHANNELS = COLOR_1 | COLOR_2 | COLOR_3;

    private static final PieceType[] ORBS = {ORB_1, ORB_2, ORB_3};
    private static final int[] CHANNEL_BITS = {COLOR_1, COLOR_2, COLOR_3};

    public int colorMask() {
        int ch = orbChannel();
        return ch < 0 ? 0 : CHANNEL_BITS[ch];
    }

    /** Tray/channel index 0..2, or {@code -1} if not an orb. */
    public int orbChannel() {
        switch (this) {
            case ORB_1:
                return 0;
            case ORB_2:
                return 1;
            case ORB_3:
                return 2;
            default:
                return -1;
        }
    }

    public boolean isOrb() {
        return orbChannel() >= 0;
    }

    /** Orb piece for tray channel {@code 0..2}. */
    public static PieceType orb(int channel) {
        if (channel < 0 || channel >= ORBS.length) {
            throw new IllegalArgumentException("channel must be 0..2, got " + channel);
        }
        return ORBS[channel];
    }

    /** Bit flag for tray channel {@code 0..2}. */
    public static int channelBit(int channel) {
        if (channel < 0 || channel >= CHANNEL_BITS.length) {
            throw new IllegalArgumentException("channel must be 0..2, got " + channel);
        }
        return CHANNEL_BITS[channel];
    }
}
