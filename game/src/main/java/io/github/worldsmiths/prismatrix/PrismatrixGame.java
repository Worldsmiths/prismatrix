package io.github.worldsmiths.prismatrix;

import io.github.worldsmiths.core.EngineServices;
import io.github.worldsmiths.core.Scene;
import io.github.worldsmiths.core.WorldsmithsGame;

/**
 * Blank Prismatrix scaffold. The engine/backend clears the window; this class sets the window
 * title and otherwise satisfies the {@link WorldsmithsGame} contract.
 */
public final class PrismatrixGame implements WorldsmithsGame {

    @Override
    public Scene createScene() {
        return new Scene();
    }

    @Override
    public void create(EngineServices services) {
        services.graphics().setTitle("Prismatrix");
    }

    @Override
    public void update(float delta) {}

    @Override
    public void render() {}

    @Override
    public void dispose() {}
}
