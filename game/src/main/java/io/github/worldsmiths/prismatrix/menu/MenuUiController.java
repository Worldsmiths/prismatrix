package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.Draw2D;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.core.ui.Button;
import io.github.worldsmiths.core.ui.Insets;
import io.github.worldsmiths.core.ui.UiBounds;
import io.github.worldsmiths.core.ui.UiHit;
import io.github.worldsmiths.core.ui.UiKit;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import java.util.HashMap;
import java.util.Map;

/**
 * Thin game-owned menu controller: viewport, clear/rebuild by phase, hit/focus helpers.
 * Builders stay pure; game owns when to clear and which phase is active.
 */
public final class MenuUiController {

    private final UiKit kit;
    private final Map<String, Button> buttonsById = new HashMap<String, Button>();
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

    /**
     * Clears kit and rebuilds leaves for {@code phase}. {@code matchPalette} and {@code
     * matchDraftTactician} feed Match settings swatch and exclusive tier leaves when phase is
     * {@link MenuPhase#MATCH_SETTINGS}.
     */
    public void rebuild(
            MenuPhase phase, OrbColorPalette.Id matchPalette, boolean matchDraftTactician) {
        if (phase == null) {
            throw new NullPointerException("phase");
        }
        buttonsById.clear();
        kit.clear();
        kit.setViewport(viewportWidth, viewportHeight, Insets.ZERO, 1f);
        switch (phase) {
            case HUB:
                rememberHub(MenuHub.build(kit));
                break;
            case WORLD_SELECT:
                rememberWorld(WorldSelect.build(kit));
                break;
            case MATCH_SETTINGS:
                rememberMatch(MatchSettings.build(kit, matchPalette, matchDraftTactician));
                break;
            case APP_SETTINGS:
                rememberSettings(AppSettings.build(kit));
                break;
            default:
                throw new IllegalStateException("unknown phase: " + phase);
        }
        kit.layout();
    }

    public void clear() {
        buttonsById.clear();
        kit.clear();
    }

    public void draw(Draw2D draw2d, Font font) {
        kit.setViewport(viewportWidth, viewportHeight, Insets.ZERO, 1f);
        kit.layout();
        kit.draw(draw2d, font);
    }

    public UiHit hit(float x, float y) {
        return kit.hit(x, y);
    }

    public void focusNext() {
        kit.focusScope().focusNext();
    }

    /**
     * Advances the focus ring until {@code id} is focused, or leaves the ring unchanged if {@code
     * id} is absent.
     */
    public void focusOn(String id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        int n = buttonsById.size();
        for (int i = 0; i < n; i++) {
            if (id.equals(kit.focusScope().focusedId())) {
                return;
            }
            kit.focusScope().focusNext();
        }
    }

    /** Updates Match Novice/Tactician XOR enabled state without rebuilding the kit. */
    public void applyMatchDraftTactician(boolean draftTactician) {
        MatchSettings.applyExclusiveTier(
                buttonsById.get(MatchSettings.ID_NOVICE),
                buttonsById.get(MatchSettings.ID_TACTICIAN),
                draftTactician);
    }

    /** Activates the focused enabled button and returns its id, or null if none. */
    public String activateFocusedId() {
        String id = kit.focusScope().focusedId();
        if (id == null) {
            return null;
        }
        Button button = buttonsById.get(id);
        if (button == null || !button.enabled()) {
            return null;
        }
        kit.focusScope().activateFocused();
        return id;
    }

    public UiBounds buttonBounds(String id) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        Button button = buttonsById.get(id);
        if (button == null) {
            throw new IllegalArgumentException("unknown menu button id: " + id);
        }
        return button.bounds();
    }

    private void rememberHub(MenuHub.HubLeaves leaves) {
        buttonsById.put(leaves.puzzle.id(), leaves.puzzle);
        buttonsById.put(leaves.pvp.id(), leaves.pvp);
        buttonsById.put(leaves.settings.id(), leaves.settings);
    }

    private void rememberWorld(WorldSelect.WorldLeaves leaves) {
        buttonsById.put(leaves.world1.id(), leaves.world1);
        buttonsById.put(leaves.back.id(), leaves.back);
    }

    private void rememberMatch(MatchSettings.MatchLeaves leaves) {
        buttonsById.put(leaves.start.id(), leaves.start);
        buttonsById.put(leaves.novice.id(), leaves.novice);
        buttonsById.put(leaves.tactician.id(), leaves.tactician);
        buttonsById.put(leaves.colors.id(), leaves.colors);
    }

    private void rememberSettings(AppSettings.SettingsLeaves leaves) {
        buttonsById.put(leaves.back.id(), leaves.back);
    }
}
