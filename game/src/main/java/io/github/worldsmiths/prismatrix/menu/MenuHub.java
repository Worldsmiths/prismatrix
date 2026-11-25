package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.ui.Button;
import io.github.worldsmiths.core.ui.Panel;
import io.github.worldsmiths.core.ui.Theme;
import io.github.worldsmiths.core.ui.TraySlot;
import io.github.worldsmiths.core.ui.UiKit;

/** Pure builder for the hub tray (Puzzle / PvP / Settings). */
public final class MenuHub {

    public static final String ID_TRAY = "hub-tray";
    public static final String ID_PUZZLE = "hub-puzzle";
    public static final String ID_PVP = "hub-pvp";
    public static final String ID_SETTINGS = "hub-settings";
    public static final String ID_TITLE_PANEL = "hub-title-panel";
    public static final String ID_TITLE = "hub-title";

    public static final String TITLE_TEXT = "Prismatrix";
    public static final String LABEL_PUZZLE = "Puzzle";
    public static final String LABEL_PVP = "PvP";
    public static final String LABEL_SETTINGS = "Settings";

    private MenuHub() {}

    /** Builds hub leaves into {@code kit} and returns tray buttons for bounds/focus. */
    public static HubLeaves build(UiKit kit) {
        if (kit == null) {
            throw new NullPointerException("kit");
        }
        kit.setTheme(Theme.defaults());
        Panel titlePanel = kit.panel(ID_TITLE_PANEL);
        titlePanel.label(ID_TITLE, TITLE_TEXT);
        TraySlot tray = kit.traySlot(ID_TRAY).length(3);
        Button puzzle = tray.slot(0, ID_PUZZLE, LABEL_PUZZLE);
        Button pvp = tray.slot(1, ID_PVP, LABEL_PVP);
        Button settings = tray.slot(2, ID_SETTINGS, LABEL_SETTINGS);
        kit.focusScope().add(puzzle);
        kit.focusScope().add(pvp);
        kit.focusScope().add(settings);
        return new HubLeaves(puzzle, pvp, settings);
    }

    /** Hub tray buttons after {@link #build(UiKit)}. */
    public static final class HubLeaves {
        public final Button puzzle;
        public final Button pvp;
        public final Button settings;

        HubLeaves(Button puzzle, Button pvp, Button settings) {
            this.puzzle = puzzle;
            this.pvp = pvp;
            this.settings = settings;
        }
    }
}
