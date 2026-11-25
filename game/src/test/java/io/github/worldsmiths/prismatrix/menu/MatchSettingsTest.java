package io.github.worldsmiths.prismatrix.menu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.core.ui.FakeUiKit;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import org.junit.jupiter.api.Test;

class MatchSettingsTest {

    @Test
    void buildDisablesSelectedNoviceByDefault() {
        FakeUiKit kit = new FakeUiKit();
        seedMeasures(kit);

        MatchSettings.MatchLeaves leaves =
                MatchSettings.build(kit, OrbColorPalette.Id.RED_YELLOW_BLUE, false);

        assertFalse(leaves.novice.enabled());
        assertTrue(leaves.tactician.enabled());
        assertTrue(leaves.start.enabled());
        assertTrue(leaves.colors.enabled());
    }

    @Test
    void buildDisablesSelectedTacticianWhenDraftTrue() {
        FakeUiKit kit = new FakeUiKit();
        seedMeasures(kit);

        MatchSettings.MatchLeaves leaves =
                MatchSettings.build(kit, OrbColorPalette.Id.RED_GREEN_BLUE, true);

        assertTrue(leaves.novice.enabled());
        assertFalse(leaves.tactician.enabled());
    }

    @Test
    void applyExclusiveTierFlipsXorEnabledState() {
        FakeUiKit kit = new FakeUiKit();
        seedMeasures(kit);
        MatchSettings.MatchLeaves leaves =
                MatchSettings.build(kit, OrbColorPalette.Id.RED_YELLOW_BLUE, false);

        MatchSettings.applyExclusiveTier(leaves.novice, leaves.tactician, true);

        assertTrue(leaves.novice.enabled());
        assertFalse(leaves.tactician.enabled());
    }

    private static void seedMeasures(FakeUiKit kit) {
        kit.seedMeasureAdvance(MatchSettings.TITLE_TEXT, 112f);
        kit.seedMeasureAdvance(MatchSettings.LABEL_START, 40f);
        kit.seedMeasureAdvance(MatchSettings.LABEL_NOVICE, 48f);
        kit.seedMeasureAdvance(MatchSettings.LABEL_TACTICIAN, 72f);
        kit.seedMeasureAdvance(MatchSettings.LABEL_COLORS, 48f);
    }
}
