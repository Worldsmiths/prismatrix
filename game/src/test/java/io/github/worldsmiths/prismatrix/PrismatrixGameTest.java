package io.github.worldsmiths.prismatrix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.worldsmiths.core.FakeEngineServices;
import io.github.worldsmiths.core.FakeInput;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.Scene;
import io.github.worldsmiths.prismatrix.menu.MenuPhase;
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
        FakeEngineServices services = new FakeEngineServices();
        services.files().putInternal(PrismatrixGame.FIRST_LEVEL_PATH, MINIMAL_LEVEL);
        services.ui().putFont(PrismatrixGame.HUD_FONT);
        services.graphics().setSize(800, 600);

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
    void num1EntersPuzzleLoadingFirstLevelPath() {
        FakeEngineServices services = new FakeEngineServices();
        services.files().putInternal(PrismatrixGame.FIRST_LEVEL_PATH, MINIMAL_LEVEL);
        services.ui().putFont(PrismatrixGame.HUD_FONT);
        services.graphics().setSize(800, 600);

        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        FakeInput input = services.input();
        input.setKeyDown(Key.NUM_1, true);
        assertDoesNotThrow(() -> game.update(0.016f));
        assertDoesNotThrow(() -> game.render());
        assertDoesNotThrow(game::dispose);
    }

    @Test
    void num2EntersPvpVsNovice() {
        FakeEngineServices services = new FakeEngineServices();
        services.ui().putFont(PrismatrixGame.HUD_FONT);
        services.graphics().setSize(800, 600);

        PrismatrixGame game = new PrismatrixGame();
        game.create(services);

        FakeInput input = services.input();
        input.setKeyDown(Key.NUM_2, true);
        assertDoesNotThrow(() -> game.update(0.016f));
        assertDoesNotThrow(() -> game.render());
        assertDoesNotThrow(game::dispose);
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
        FakeInput input = services.input();

        game.goToMenuPhase(MenuPhase.WORLD_SELECT);
        input.setKeyDown(Key.NUM_1, true);
        game.update(0.016f);
        input.setKeyDown(Key.NUM_1, false);
        game.update(0.016f);
        assertEquals(PrismatrixGame.PlayMode.PUZZLE, game.playMode());

        pressEscape(input, game);
        assertEquals(PrismatrixGame.PlayMode.MENU, game.playMode());
        assertEquals(MenuPhase.HUB, game.menuPhase());

        game.goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        input.setKeyDown(Key.NUM_2, true);
        game.update(0.016f);
        input.setKeyDown(Key.NUM_2, false);
        game.update(0.016f);
        assertEquals(PrismatrixGame.PlayMode.PVP, game.playMode());

        pressEscape(input, game);
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
        return services;
    }

    private static void pressEscape(FakeInput input, PrismatrixGame game) {
        input.setKeyDown(Key.ESCAPE, true);
        game.update(0.016f);
        input.setKeyDown(Key.ESCAPE, false);
        game.update(0.016f);
    }
}
