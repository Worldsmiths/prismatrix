package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.ui.Button;
import io.github.worldsmiths.core.ui.Panel;
import io.github.worldsmiths.core.ui.Theme;
import io.github.worldsmiths.core.ui.UiKit;

/** Pure builder for App Settings placeholder (title + Coming soon + Back). */
public final class AppSettings {

    public static final String ID_BACK = "settings-back";
    public static final String ID_TITLE_PANEL = "settings-title-panel";
    public static final String ID_TITLE = "settings-title";
    public static final String ID_COMING_SOON = "settings-coming-soon";

    public static final String TITLE_TEXT = "Settings";
    public static final String COMING_SOON_TEXT = "Coming soon";
    public static final String LABEL_BACK = "Back";

    private AppSettings() {}

    /** Builds App Settings leaves into {@code kit} and returns the Back button. */
    public static SettingsLeaves build(UiKit kit) {
        if (kit == null) {
            throw new NullPointerException("kit");
        }
        kit.setTheme(Theme.defaults());
        Panel titlePanel = kit.panel(ID_TITLE_PANEL);
        titlePanel.label(ID_TITLE, TITLE_TEXT);
        kit.label(ID_COMING_SOON, COMING_SOON_TEXT);
        Button back = kit.button(ID_BACK, LABEL_BACK);
        kit.focusScope().add(back);
        return new SettingsLeaves(back);
    }

    /** App Settings buttons after {@link #build(UiKit)}. */
    public static final class SettingsLeaves {
        public final Button back;

        SettingsLeaves(Button back) {
            this.back = back;
        }
    }
}
