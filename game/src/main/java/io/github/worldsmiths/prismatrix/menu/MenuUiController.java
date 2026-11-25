package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.ui.Insets;
import io.github.worldsmiths.core.ui.UiKit;

/**
 * Thin game-owned menu controller: viewport, clear/rebuild by phase, hit/focus wiring later.
 * Builders stay pure; game owns when to clear and which phase is active.
 */
public final class MenuUiController {

    private final UiKit kit;
    private int viewportWidth = 800;
    private int viewportHeight = 600;

    public MenuUiController(UiKit kit) {
        if (kit == null) {
            throw new NullPointerException("kit");
        }
        this.kit = kit;
    }

    public void setViewport(int width, int height) {
        viewportWidth = width > 0 ? width : 800;
        viewportHeight = height > 0 ? height : 600;
    }

    /** Clears kit and rebuilds leaves for {@code phase}. */
    public void rebuild(MenuPhase phase) {
        if (phase == null) {
            throw new NullPointerException("phase");
        }
        kit.clear();
        kit.setViewport(viewportWidth, viewportHeight, Insets.ZERO, 1f);
        switch (phase) {
            case HUB:
                MenuHub.build(kit);
                break;
            case WORLD_SELECT:
                WorldSelect.build(kit);
                break;
            case MATCH_SETTINGS:
                MatchSettings.build(kit);
                break;
            case APP_SETTINGS:
                AppSettings.build(kit);
                break;
            default:
                throw new IllegalStateException("unknown phase: " + phase);
        }
        kit.layout();
    }

    public void clear() {
        kit.clear();
    }
}
