package io.github.worldsmiths.prismatrix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.core.FakeEngineServices;
import io.github.worldsmiths.core.FakeInput;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.Scene;
import io.github.worldsmiths.core.ui.FakeUiKit;
import io.github.worldsmiths.core.ui.UiBounds;
import io.github.worldsmiths.prismatrix.menu.AppSettings;
import io.github.worldsmiths.prismatrix.menu.MatchSettings;
import io.github.worldsmiths.prismatrix.menu.MenuHub;
import io.github.worldsmiths.prismatrix.menu.MenuPhase;
import io.github.worldsmiths.prismatrix.menu.WorldSelect;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrismatrixGameTest {

    private static final String MINIMAL_LEVEL =
            "{"
                    + "\"id\":\"w1-01\","
                    + "\"gridSize\":6,"
                    + "\"mirrorBudget\":1,"
                    + "\"goals\":["
                    + "{\"entryPort\":\"T2\",\"exitPort\":\"L2\",\"colorMask\":0}"
                    + "]"
                    + "}";

    @Test
    void createSceneLifecycleSetsPrismatrixTitle() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();

        Scene scene = assertDoesNotThrow(game::createScene);
        assertDoesNotThrow(() -> game.create(services));
        assertDoesNotThrow(() -> game.update(0.016f));
        assertDoesNotThrow(() -> game.render());
        assertDoesNotThrow(game::dispose);

        assertEquals("Prismatrix", services.graphics().lastTitle());
        assertEquals(Scene.class, scene.getClass());
    }

    @Test
    void pointerOnHubPuzzleOpensWorldSelect() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        tapMenuButton(services.input(), game, MenuHub.ID_PUZZLE);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.WORLD_SELECT, game.menuPhase());

        game.dispose();
    }

    @Test
    void pointerOnHubPvpOpensMatchSettings() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        tapMenuButton(services.input(), game, MenuHub.ID_PVP);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.MATCH_SETTINGS, game.menuPhase());

        game.dispose();
    }

    @Test
    void pointerOnHubSettingsOpensAppSettings() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        tapMenuButton(services.input(), game, MenuHub.ID_SETTINGS);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.APP_SETTINGS, game.menuPhase());

        game.dispose();
    }

    @Test
    void focusActivateOnHubPuzzleOpensWorldSelect() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        pressKey(services.input(), game, Key.ENTER);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.WORLD_SELECT, game.menuPhase());

        game.dispose();
    }

    @Test
    void focusTabThenSpaceOpensMatchSettings() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.SPACE);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.MATCH_SETTINGS, game.menuPhase());

        game.dispose();
    }

    @Test
    void focusTabTwiceThenEnterOpensAppSettings() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.ENTER);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.APP_SETTINGS, game.menuPhase());

        game.dispose();
    }

    @Test
    void digitsOnHubOpenStubPhases() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        pressKey(input, game, Key.NUM_1);
        assertEquals(MenuPhase.WORLD_SELECT, game.menuPhase());
        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());

        game.goToMenuPhase(MenuPhase.HUB);
        pressKey(input, game, Key.NUM_2);
        assertEquals(MenuPhase.MATCH_SETTINGS, game.menuPhase());
        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());

        game.goToMenuPhase(MenuPhase.HUB);
        pressKey(input, game, Key.NUM_3);
        assertEquals(MenuPhase.APP_SETTINGS, game.menuPhase());
        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());

        game.dispose();
    }

    @Test
    void digitsOnStubPhasesAreNoOp() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        pressKey(input, game, Key.NUM_1);
        pressKey(input, game, Key.NUM_2);
        pressKey(input, game, Key.NUM_3);

        assertEquals(MenuPhase.MATCH_SETTINGS, game.menuPhase());
        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());

        game.dispose();
    }

    @Test
    void keyCDoesNotToggleMenuOrbColors() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        assertEquals(OrbColorPalette.Id.RED_YELLOW_BLUE, game.menuOrbColors());
        pressKey(input, game, Key.C);
        assertEquals(OrbColorPalette.Id.RED_YELLOW_BLUE, game.menuOrbColors());

        game.dispose();
    }

    @Test
    void pointerOnWorld1EntersPuzzle() {
        FakeEngineServices services = bootServices(true);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        game.goToMenuPhase(MenuPhase.WORLD_SELECT);
        tapMenuButton(services.input(), game, WorldSelect.ID_WORLD_1);

        assertEquals(PrismatrixGame.PlayMode.PUZZLE, game.playMode());

        game.dispose();
    }

    @Test
    void focusActivateOnWorld1EntersPuzzle() {
        FakeEngineServices services = bootServices(true);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        game.goToMenuPhase(MenuPhase.WORLD_SELECT);
        pressKey(services.input(), game, Key.ENTER);

        assertEquals(PrismatrixGame.PlayMode.PUZZLE, game.playMode());

        game.dispose();
    }

    @Test
    void pointerOnWorldBackReturnsToHub() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        game.goToMenuPhase(MenuPhase.WORLD_SELECT);
        tapMenuButton(services.input(), game, WorldSelect.ID_BACK);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        game.dispose();
    }

    @Test
    void focusActivateOnWorldBackReturnsToHub() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.WORLD_SELECT);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.ENTER);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        game.dispose();
    }

    @Test
    void matchTierAndColorsWithoutStartStayInMenu() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        tapMenuButton(input, game, MatchSettings.ID_TACTICIAN);
        tapMenuButton(input, game, MatchSettings.ID_COLORS);
        tapMenuButton(input, game, MatchSettings.ID_NOVICE);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.MATCH_SETTINGS, game.menuPhase());
        assertEquals(OrbColorPalette.Id.RED_GREEN_BLUE, game.menuOrbColors());

        game.dispose();
    }

    @Test
    void matchStartAfterNoviceEntersPvpNovice() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        tapMenuButton(input, game, MatchSettings.ID_NOVICE);
        tapMenuButton(input, game, MatchSettings.ID_START);

        assertEquals(PrismatrixGame.PlayMode.PVP, game.playMode());
        assertEquals(false, game.lastPvpTactician());
        assertEquals(OrbColorPalette.Id.RED_YELLOW_BLUE, game.menuOrbColors());

        game.dispose();
    }

    @Test
    void matchStartAfterTacticianEntersPvpTactician() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        tapMenuButton(input, game, MatchSettings.ID_TACTICIAN);
        tapMenuButton(input, game, MatchSettings.ID_START);

        assertEquals(PrismatrixGame.PlayMode.PVP, game.playMode());
        assertEquals(true, game.lastPvpTactician());

        game.dispose();
    }

    @Test
    void matchColorsThenStartAppliesToggledPalette() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        assertEquals(OrbColorPalette.Id.RED_YELLOW_BLUE, game.menuOrbColors());
        tapMenuButton(input, game, MatchSettings.ID_COLORS);
        assertEquals(OrbColorPalette.Id.RED_GREEN_BLUE, game.menuOrbColors());
        tapMenuButton(input, game, MatchSettings.ID_START);

        assertEquals(PrismatrixGame.PlayMode.PVP, game.playMode());
        assertEquals(OrbColorPalette.Id.RED_GREEN_BLUE, game.menuOrbColors());
        assertEquals(false, game.lastPvpTactician());

        game.dispose();
    }

    @Test
    void escapeFromMatchSettingsReturnsToHub() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        pressEscape(services.input(), game);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        game.dispose();
    }

    @Test
    void focusActivateOnMatchStartEntersPvpNovice() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        pressKey(services.input(), game, Key.ENTER);

        assertEquals(PrismatrixGame.PlayMode.PVP, game.playMode());
        assertEquals(false, game.lastPvpTactician());
        assertEquals(OrbColorPalette.Id.RED_YELLOW_BLUE, game.menuOrbColors());

        game.dispose();
    }

    @Test
    void focusActivateMatchTacticianThenStartEntersPvpTactician() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        // Focus ring: Start → Novice → Tactician → Colors
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.ENTER);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.ENTER);

        assertEquals(PrismatrixGame.PlayMode.PVP, game.playMode());
        assertEquals(true, game.lastPvpTactician());

        game.dispose();
    }

    @Test
    void focusActivateMatchColorsTogglesPaletteWithoutLeavingMenu() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.ENTER);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.MATCH_SETTINGS, game.menuPhase());
        assertEquals(OrbColorPalette.Id.RED_GREEN_BLUE, game.menuOrbColors());

        game.dispose();
    }

    @Test
    void focusActivateMatchColorsThenEnterOrSpaceStaysInMenu() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        // Focus ring: Start → Novice → Tactician → Colors
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.TAB);
        pressKey(input, game, Key.ENTER);
        assertEquals(OrbColorPalette.Id.RED_GREEN_BLUE, game.menuOrbColors());

        // Rebuild must not leave focus on Start; following Enter/Space must not commit PvP.
        pressKey(input, game, Key.ENTER);
        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.MATCH_SETTINGS, game.menuPhase());
        assertEquals(OrbColorPalette.Id.RED_YELLOW_BLUE, game.menuOrbColors());

        pressKey(input, game, Key.SPACE);
        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.MATCH_SETTINGS, game.menuPhase());
        assertEquals(OrbColorPalette.Id.RED_GREEN_BLUE, game.menuOrbColors());

        game.dispose();
    }

    @Test
    void matchTacticianThenColorsThenStartKeepsTacticianDraft() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        tapMenuButton(input, game, MatchSettings.ID_TACTICIAN);
        tapMenuButton(input, game, MatchSettings.ID_COLORS);
        tapMenuButton(input, game, MatchSettings.ID_START);

        assertEquals(PrismatrixGame.PlayMode.PVP, game.playMode());
        assertEquals(true, game.lastPvpTactician());
        assertEquals(OrbColorPalette.Id.RED_GREEN_BLUE, game.menuOrbColors());

        game.dispose();
    }

    @Test
    void hubPointerAndFocusRecordFakeUiKitHitAndActivateIds() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeUiKit uiKit = services.uiKit();

        assertTrue(
                containsCommand(uiKit.commands(), FakeUiKit.Command.Kind.CREATE, MenuHub.ID_PUZZLE),
                "CREATE hub-puzzle");
        assertTrue(
                containsCommand(uiKit.commands(), FakeUiKit.Command.Kind.CREATE, MenuHub.ID_PVP),
                "CREATE hub-pvp");
        assertTrue(
                containsCommand(
                        uiKit.commands(), FakeUiKit.Command.Kind.CREATE, MenuHub.ID_SETTINGS),
                "CREATE hub-settings");

        uiKit.clearCommands();
        tapMenuButton(services.input(), game, MenuHub.ID_PUZZLE);
        assertTrue(
                containsCommand(uiKit.commands(), FakeUiKit.Command.Kind.HIT, MenuHub.ID_PUZZLE),
                "HIT hub-puzzle");
        assertEquals(MenuPhase.WORLD_SELECT, game.menuPhase());

        game.goToMenuPhase(MenuPhase.HUB);
        uiKit.clearCommands();
        pressKey(services.input(), game, Key.ENTER);
        assertTrue(
                containsCommand(
                        uiKit.commands(), FakeUiKit.Command.Kind.ACTIVATE, MenuHub.ID_PUZZLE),
                "ACTIVATE hub-puzzle");
        assertEquals(MenuPhase.WORLD_SELECT, game.menuPhase());

        game.dispose();
    }

    @Test
    void pointerOnSettingsBackReturnsToHub() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        game.goToMenuPhase(MenuPhase.APP_SETTINGS);
        tapMenuButton(services.input(), game, AppSettings.ID_BACK);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        game.dispose();
    }

    @Test
    void focusActivateOnSettingsBackReturnsToHub() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        game.goToMenuPhase(MenuPhase.APP_SETTINGS);
        pressKey(services.input(), game, Key.ENTER);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        game.dispose();
    }

    @Test
    void escapeFromStubPhasesReturnsToHub() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        for (MenuPhase stub :
                new MenuPhase[] {
                    MenuPhase.WORLD_SELECT, MenuPhase.MATCH_SETTINGS, MenuPhase.APP_SETTINGS
                }) {
            game.goToMenuPhase(stub);
            assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
            assertEquals(stub, game.menuPhase());

            pressEscape(input, game);

            assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
            assertEquals(MenuPhase.HUB, game.menuPhase());
        }

        game.dispose();
    }

    @Test
    void escapeFromPlayReturnsToMenuHubNotPriorStub() {
        FakeEngineServices services = bootServices(true);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        game.goToMenuPhase(MenuPhase.WORLD_SELECT);
        game.enterPuzzleForTest();
        assertEquals(PrismatrixGame.PlayMode.PUZZLE, game.playMode());

        pressEscape(services.input(), game);
        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        game.enterPvpForTest(false);
        assertEquals(PrismatrixGame.PlayMode.PVP, game.playMode());

        pressEscape(services.input(), game);
        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        game.dispose();
    }

    @Test
    void escapeOnHubIsNoOp() {
        FakeEngineServices services = bootServices(false);
        PrismatrixGame game = new PrismatrixGame();
        game.create(services);
        FakeInput input = services.input();

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        pressEscape(input, game);

        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        game.dispose();
    }

    private static FakeEngineServices bootServices(boolean withLevel) {
        FakeEngineServices services = new FakeEngineServices();
        if (withLevel) {
            services.files().putInternal(PrismatrixGame.FIRST_LEVEL_PATH, MINIMAL_LEVEL);
        }
        services.ui().putFont(PrismatrixGame.HUD_FONT);
        services.graphics().setSize(800, 600);
        seedHubMeasures(services.uiKit());
        return services;
    }

    private static void seedHubMeasures(FakeUiKit uiKit) {
        uiKit.seedMeasureAdvance(MenuHub.TITLE_TEXT, 96f);
        uiKit.seedMeasureAdvance(MenuHub.LABEL_PUZZLE, 48f);
        uiKit.seedMeasureAdvance(MenuHub.LABEL_PVP, 32f);
        uiKit.seedMeasureAdvance(MenuHub.LABEL_SETTINGS, 64f);
        uiKit.seedMeasureAdvance(WorldSelect.TITLE_TEXT, 96f);
        uiKit.seedMeasureAdvance(WorldSelect.LABEL_WORLD_1, 56f);
        uiKit.seedMeasureAdvance(WorldSelect.LABEL_BACK, 40f);
        uiKit.seedMeasureAdvance(AppSettings.COMING_SOON_TEXT, 88f);
        uiKit.seedMeasureAdvance(MatchSettings.TITLE_TEXT, 112f);
        uiKit.seedMeasureAdvance(MatchSettings.LABEL_START, 40f);
        uiKit.seedMeasureAdvance(MatchSettings.LABEL_NOVICE, 48f);
        uiKit.seedMeasureAdvance(MatchSettings.LABEL_TACTICIAN, 72f);
        uiKit.seedMeasureAdvance(MatchSettings.LABEL_COLORS, 48f);
    }

    private static void tapMenuButton(FakeInput input, PrismatrixGame game, String leafId) {
        UiBounds bounds = game.menuButtonBounds(leafId);
        input.setPointer((int) bounds.centerX(), (int) bounds.centerY());
        input.setJustTouched(true);
        game.update(0.016f);
        input.setJustTouched(false);
    }

    private static void pressEscape(FakeInput input, PrismatrixGame game) {
        pressKey(input, game, Key.ESCAPE);
    }

    private static void pressKey(FakeInput input, PrismatrixGame game, Key key) {
        input.setKeyDown(key, true);
        game.update(0.016f);
        input.setKeyDown(key, false);
        game.update(0.016f);
    }

    private static boolean containsCommand(
            List<FakeUiKit.Command> commands, FakeUiKit.Command.Kind kind, String id) {
        for (FakeUiKit.Command command : commands) {
            if (command.kind == kind && id.equals(command.id)) {
                return true;
            }
        }
        return false;
    }
}
