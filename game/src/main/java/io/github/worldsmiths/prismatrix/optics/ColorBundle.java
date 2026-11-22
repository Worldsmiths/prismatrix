package io.github.worldsmiths.prismatrix.optics;

/**
 * Named primary + mix table for one orb palette. Optics still use channel bits {@link
 * PieceType#COLOR_1}/{@link PieceType#COLOR_2}/{@link PieceType#COLOR_3}; this bundle only defines
 * how those bits look and are labeled.
 *
 * <p>Channels bind in tray order: channel 0 → {@code COLOR_1}, channel 1 → {@code COLOR_2}, channel
 * 2 → {@code COLOR_3}. Pair mixes are 0+1, 1+2, 0+2; {@code full} is all three. Source (mask 0) is
 * always white.
 */
public final class ColorBundle {

    /** Source beam / empty mask — always pure white. */
    public static final int SOURCE_WHITE_RGB = 0xFFFFFF;

    private final String displayName;
    private final String[] channelNames;
    private final int[] primaryRgb;
    private final int pair01Rgb;
    private final String pair01Label;
    private final int pair12Rgb;
    private final String pair12Label;
    private final int pair02Rgb;
    private final String pair02Label;
    private final int fullRgb;
    private final String fullLabel;

    public ColorBundle(
            String displayName,
            String[] channelNames,
            int[] primaryRgb,
            int pair01Rgb,
            String pair01Label,
            int pair12Rgb,
            String pair12Label,
            int pair02Rgb,
            String pair02Label,
            int fullRgb,
            String fullLabel) {
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("displayName required");
        }
        if (channelNames == null || channelNames.length != 3) {
            throw new IllegalArgumentException("channelNames must have length 3");
        }
        if (primaryRgb == null || primaryRgb.length != 3) {
            throw new IllegalArgumentException("primaryRgb must have length 3");
        }
        requireLabel(pair01Label, "pair01Label");
        requireLabel(pair12Label, "pair12Label");
        requireLabel(pair02Label, "pair02Label");
        requireLabel(fullLabel, "fullLabel");
        this.displayName = displayName;
        this.channelNames = channelNames.clone();
        this.primaryRgb = primaryRgb.clone();
        this.pair01Rgb = pair01Rgb;
        this.pair01Label = pair01Label;
        this.pair12Rgb = pair12Rgb;
        this.pair12Label = pair12Label;
        this.pair02Rgb = pair02Rgb;
        this.pair02Label = pair02Label;
        this.fullRgb = fullRgb;
        this.fullLabel = fullLabel;
    }

    /**
     * Easy RYB-style paint mixes. Tray: red, yellow, blue.
     *
     * <pre>
     * 1+2=#FF6600 orange · 2+3=#008000 green · 1+3=#800080 purple · all=#804000 brown
     * </pre>
     */
    public static ColorBundle redYellowBlue() {
        return new ColorBundle(
                "Red / Yellow / Blue",
                new String[] {"red", "yellow", "blue"},
                new int[] {0xFF0000, 0xFFFF00, 0x0000FF},
                0xFF6600,
                "orange",
                0x008000,
                "green",
                0x800080,
                "purple",
                0x663300,
                "brown");
    }

    /**
     * Hard RGB-light mixes. Tray: red, green, blue.
     *
     * <pre>
     * 1+2=#FFFF00 yellow · 2+3=#00FFFF cyan · 1+3=#FF00FF magenta · all=#FFFFFF white
     * </pre>
     */
    public static ColorBundle redGreenBlue() {
        return new ColorBundle(
                "Red / Green / Blue",
                new String[] {"red", "green", "blue"},
                new int[] {0xFF0000, 0x00FF00, 0x0000FF},
                0xFFFF00,
                "yellow",
                0x00FFFF,
                "cyan",
                0xFF00FF,
                "magenta",
                0xFFFFFF,
                "white");
    }

    public String displayName() {
        return displayName;
    }

    public String channelName(int channel) {
        return channelNames[channel];
    }

    public String[] channelNames() {
        return channelNames.clone();
    }

    public int primaryRgb(int channel) {
        return primaryRgb[channel];
    }

    public int[] primaryRgb() {
        return primaryRgb.clone();
    }

    public int pair01Rgb() {
        return pair01Rgb;
    }

    public String pair01Label() {
        return pair01Label;
    }

    public int pair12Rgb() {
        return pair12Rgb;
    }

    public String pair12Label() {
        return pair12Label;
    }

    public int pair02Rgb() {
        return pair02Rgb;
    }

    public String pair02Label() {
        return pair02Label;
    }

    public int fullRgb() {
        return fullRgb;
    }

    public String fullLabel() {
        return fullLabel;
    }

    private static void requireLabel(String label, String field) {
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException(field + " required");
        }
    }
}
