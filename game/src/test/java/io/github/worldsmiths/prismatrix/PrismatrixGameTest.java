package io.github.worldsmiths.prismatrix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.worldsmiths.core.FakeEngineServices;
import io.github.worldsmiths.core.FakeInput;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.Scene;
import org.junit.jupiter.api.Test;

class PrismatrixGameTest {

    private static final String MINIMAL_LEVEL =
            "{"
                    + "\"id\":\"w1-01\","
                    + "\"gridSize\":6,"
                    + "\"mirrorBudget\":1,"
                    + "\"clues\":["
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
}
