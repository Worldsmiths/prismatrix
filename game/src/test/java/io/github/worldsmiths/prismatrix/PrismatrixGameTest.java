package io.github.worldsmiths.prismatrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.github.worldsmiths.core.FakeEngineServices;
import io.github.worldsmiths.core.Scene;
import org.junit.jupiter.api.Test;

class PrismatrixGameTest {

    @Test
    void createSceneLifecycleSetsPrismatrixTitle() {
        FakeEngineServices services = new FakeEngineServices();
        PrismatrixGame game = new PrismatrixGame();

        Scene scene = assertDoesNotThrow(game::createScene);
        assertDoesNotThrow(() -> game.create(services));
        assertDoesNotThrow(() -> game.update(0.016f));
        assertDoesNotThrow(() -> game.render());
        assertDoesNotThrow(game::dispose);

        assertEquals("Prismatrix", services.graphics().lastTitle());
        assertEquals(Scene.class, scene.getClass());
    }
}
