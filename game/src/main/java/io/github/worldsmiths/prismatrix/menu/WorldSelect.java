package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.ui.Button;
import io.github.worldsmiths.core.ui.Panel;
import io.github.worldsmiths.core.ui.Theme;
import io.github.worldsmiths.core.ui.TraySlot;
import io.github.worldsmiths.core.ui.UiKit;

/** Pure builder for World Select (World 1 + Back). */
public final class WorldSelect {

    public static final String ID_TRAY = "world-tray";
    public static final String ID_WORLD_1 = "world-1";
    public static final String ID_BACK = "world-back";
    public static final String ID_TITLE_PANEL = "world-title-panel";
    public static final String ID_TITLE = "world-title";

    public static final String TITLE_TEXT = "World Select";
    public static final String LABEL_WORLD_1 = "World 1";
    public static final String LABEL_BACK = "Back";

    private WorldSelect() {}

    /** Builds World Select leaves into {@code kit} and returns hittable buttons. */
    public static WorldLeaves build(UiKit kit) {
        if (kit == null) {
            throw new NullPointerException("kit");
        }
        kit.setTheme(Theme.defaults());
        Panel titlePanel = kit.panel(ID_TITLE_PANEL);
        titlePanel.label(ID_TITLE, TITLE_TEXT);
        Button world1 = kit.button(ID_WORLD_1, LABEL_WORLD_1);
        TraySlot tray = kit.traySlot(ID_TRAY).length(1);
        Button back = tray.slot(0, ID_BACK, LABEL_BACK);
        kit.focusScope().add(world1);
        kit.focusScope().add(back);
        return new WorldLeaves(world1, back);
    }

    /** World Select buttons after {@link #build(UiKit)}. */
    public static final class WorldLeaves {
        public final Button world1;
        public final Button back;

        WorldLeaves(Button world1, Button back) {
            this.world1 = world1;
            this.back = back;
        }
    }
}
