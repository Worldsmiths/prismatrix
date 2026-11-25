package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.ui.Button;
import io.github.worldsmiths.core.ui.Panel;
import io.github.worldsmiths.core.ui.Swatch;
import io.github.worldsmiths.core.ui.Theme;
import io.github.worldsmiths.core.ui.TraySlot;
import io.github.worldsmiths.core.ui.UiKit;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;

/** Pure builder for Match settings (Start + Novice / Tactician / Colors). */
public final class MatchSettings {

    public static final String ID_START = "match-start";
    public static final String ID_TRAY = "match-tray";
    public static final String ID_NOVICE = "match-novice";
    public static final String ID_TACTICIAN = "match-tactician";
    public static final String ID_COLORS = "match-colors";
    public static final String ID_TITLE_PANEL = "match-title-panel";
    public static final String ID_TITLE = "match-title";
    public static final String ID_SWATCH = "match-swatch";

    public static final String TITLE_TEXT = "Match settings";
    public static final String LABEL_START = "Start";
    public static final String LABEL_NOVICE = "Novice";
    public static final String LABEL_TACTICIAN = "Tactician";
    public static final String LABEL_COLORS = "Colors";

    private MatchSettings() {}

    /**
     * Builds Match settings leaves into {@code kit} and returns hittable buttons. Optional swatch
     * reflects {@code palette} glance color.
     */
    public static MatchLeaves build(UiKit kit, OrbColorPalette.Id palette) {
        if (kit == null) {
            throw new NullPointerException("kit");
        }
        OrbColorPalette.Id colors =
                palette == null ? OrbColorPalette.Id.RED_YELLOW_BLUE : palette;
        kit.setTheme(Theme.defaults());
        Panel titlePanel = kit.panel(ID_TITLE_PANEL);
        titlePanel.label(ID_TITLE, TITLE_TEXT);
        Button start = kit.button(ID_START, LABEL_START);
        TraySlot tray = kit.traySlot(ID_TRAY).length(3);
        Button novice = tray.slot(0, ID_NOVICE, LABEL_NOVICE);
        Button tactician = tray.slot(1, ID_TACTICIAN, LABEL_TACTICIAN);
        Button colorsButton = tray.slot(2, ID_COLORS, LABEL_COLORS);
        float[] rgba = OrbColorPalette.of(colors).channelRgba(0);
        Swatch swatch = kit.swatch(ID_SWATCH);
        swatch.color(rgba[0], rgba[1], rgba[2], rgba[3]);
        kit.focusScope().add(start);
        kit.focusScope().add(novice);
        kit.focusScope().add(tactician);
        kit.focusScope().add(colorsButton);
        return new MatchLeaves(start, novice, tactician, colorsButton);
    }

    /** Match settings buttons after {@link #build(UiKit, OrbColorPalette.Id)}. */
    public static final class MatchLeaves {
        public final Button start;
        public final Button novice;
        public final Button tactician;
        public final Button colors;

        MatchLeaves(Button start, Button novice, Button tactician, Button colors) {
            this.start = start;
            this.novice = novice;
            this.tactician = tactician;
            this.colors = colors;
        }
    }
}
