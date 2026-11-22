package io.github.worldsmiths.prismatrix.optics;

/**
 * Presentation mapping from Trace color-mask bits ({@link PieceType#COLOR_1}/{@code COLOR_2}/{@code
 * COLOR_3}) to human colors. Optics stay bitflags; this only affects labels, beams, ports, and orb
 * meshes — always via an explicit {@link ColorBundle} mix table (never ad-hoc RGB blending).
 *
 * <ul>
 *   <li>{@link Id#RED_YELLOW_BLUE} (default, easy) — tray R,Y,B; 1+2=orange, 2+3=green, 1+3=purple,
 *       all=brown.
 *   <li>{@link Id#RED_GREEN_BLUE} (hard) — tray R,G,B; 1+2=yellow, 2+3=cyan, 1+3=magenta, all=white.
 * </ul>
 */
public final class OrbColorPalette {

    /** Bits for all three primary channels. */
    public static final int FULL_SPECTRUM = PieceType.ALL_CHANNELS;

    private static final int[] CHANNEL_BITS =
            new int[] {PieceType.COLOR_1, PieceType.COLOR_2, PieceType.COLOR_3};

    public enum Id {
        /** Red / Yellow / Blue on ORB_1 / ORB_2 / ORB_3. Full mix brown (≠ start white). */
        RED_YELLOW_BLUE,
        /** Red / Green / Blue on ORB_1 / ORB_2 / ORB_3. Full mix white (= start white). */
        RED_GREEN_BLUE;
    }

    private final Id id;
    private final ColorBundle bundle;
    /** Mask 0..7 → RGBA. */
    private final float[][] lut;
    /** Mask 0..7 → HUD label. */
    private final String[] labels;

    private OrbColorPalette(Id id, ColorBundle bundle, float[][] lut, String[] labels) {
        this.id = id;
        this.bundle = bundle;
        this.lut = lut;
        this.labels = labels;
    }

    /** Easy RYB-style preset from {@link ColorBundle#redYellowBlue()}. */
    public static OrbColorPalette redYellowBlue() {
        return fromBundle(Id.RED_YELLOW_BLUE, ColorBundle.redYellowBlue());
    }

    /** Hard RGB-light preset from {@link ColorBundle#redGreenBlue()}. */
    public static OrbColorPalette redGreenBlue() {
        return fromBundle(Id.RED_GREEN_BLUE, ColorBundle.redGreenBlue());
    }

    /** Builds a palette LUT from an explicit primary + mix bundle. */
    public static OrbColorPalette fromBundle(Id id, ColorBundle bundle) {
        if (id == null) {
            throw new IllegalArgumentException("id required");
        }
        if (bundle == null) {
            throw new IllegalArgumentException("bundle required");
        }
        float[][] lut = new float[8][4];
        String[] labels = new String[8];
        lut[0] = hex(ColorBundle.SOURCE_WHITE_RGB);
        labels[0] = "white";

        for (int i = 0; i < 3; i++) {
            lut[CHANNEL_BITS[i]] = hex(bundle.primaryRgb(i));
            labels[CHANNEL_BITS[i]] = bundle.channelName(i);
        }

        int bit01 = CHANNEL_BITS[0] | CHANNEL_BITS[1];
        int bit12 = CHANNEL_BITS[1] | CHANNEL_BITS[2];
        int bit02 = CHANNEL_BITS[0] | CHANNEL_BITS[2];

        lut[bit01] = hex(bundle.pair01Rgb());
        labels[bit01] = bundle.pair01Label();
        lut[bit12] = hex(bundle.pair12Rgb());
        labels[bit12] = bundle.pair12Label();
        lut[bit02] = hex(bundle.pair02Rgb());
        labels[bit02] = bundle.pair02Label();

        lut[FULL_SPECTRUM] = hex(bundle.fullRgb());
        labels[FULL_SPECTRUM] = bundle.fullLabel();

        return new OrbColorPalette(id, bundle, lut, labels);
    }

    public static OrbColorPalette of(Id id) {
        if (id == null) {
            return redYellowBlue();
        }
        switch (id) {
            case RED_GREEN_BLUE:
                return redGreenBlue();
            case RED_YELLOW_BLUE:
            default:
                return redYellowBlue();
        }
    }

    public static OrbColorPalette defaultPalette() {
        return redYellowBlue();
    }

    public Id id() {
        return id;
    }

    public ColorBundle bundle() {
        return bundle;
    }

    public String displayName() {
        return bundle.displayName();
    }

    public String shortLabel() {
        if (id == Id.RED_GREEN_BLUE) {
            return "RGB (hard)";
        }
        return "RYB (easy)";
    }

    public String channelName(int channel) {
        return bundle.channelName(channel);
    }

    public float[] channelRgba(int channel) {
        return rgba(CHANNEL_BITS[channel]);
    }

    public float[] rgbaForPiece(PieceType piece) {
        if (piece == null || !piece.isOrb()) {
            return rgba(0.7f, 0.7f, 0.7f);
        }
        return rgba(CHANNEL_BITS[piece.orbChannel()]);
    }

    /** Beam / exit / swatch RGBA for a Trace color mask. Mask {@code 0} → source white. */
    public float[] rgba(int colorMask) {
        int m = colorMask & FULL_SPECTRUM;
        float[] c = lut[m];
        return new float[] {c[0], c[1], c[2], c[3]};
    }

    /** HUD label: primaries, distinct mix names ({@code orange}, {@code cyan}, …), or white. */
    public String label(int colorMask) {
        return labels[colorMask & FULL_SPECTRUM];
    }

    private static float[] rgba(float r, float g, float b) {
        return new float[] {r, g, b, 1f};
    }

    /** {@code 0xRRGGBB} → RGBA floats in {@code 0..1}. */
    private static float[] hex(int rgb) {
        float r = ((rgb >> 16) & 0xff) / 255f;
        float g = ((rgb >> 8) & 0xff) / 255f;
        float b = (rgb & 0xff) / 255f;
        return rgba(r, g, b);
    }
}
