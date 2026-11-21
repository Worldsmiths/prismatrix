package io.github.worldsmiths.prismatrix;

import io.github.worldsmiths.core.EngineException;
import io.github.worldsmiths.core.EngineServices;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.core.Scene;
import io.github.worldsmiths.core.WorldsmithsGame;
import io.github.worldsmiths.prismatrix.puzzle.LevelLoadException;
import io.github.worldsmiths.prismatrix.puzzle.Presentation;
import io.github.worldsmiths.prismatrix.puzzle.PuzzleMode;

/**
 * Prismatrix desktop Puzzle W1: loads the first packaged level, drives flat P0 {@link
 * Presentation}, and holds the active {@link PuzzleMode} session.
 */
public final class PrismatrixGame implements WorldsmithsGame {

    static final String FIRST_LEVEL_PATH = "levels/w1/01.json";
    static final String HUD_FONT = "fonts/hud.fnt";

    private EngineServices services;
    private PuzzleMode puzzle;
    private Presentation presentation;
    private Font font;

    @Override
    public Scene createScene() {
        return new Scene();
    }

    @Override
    public void create(EngineServices services) {
        if (services == null) {
            throw new NullPointerException("services");
        }
        this.services = services;
        services.graphics().setTitle("Prismatrix");
        try {
            puzzle = PuzzleMode.load(services.files(), FIRST_LEVEL_PATH);
        } catch (LevelLoadException e) {
            throw new EngineException("Failed to load " + FIRST_LEVEL_PATH, e);
        }
        font = services.ui().createFont(HUD_FONT);
        presentation = new Presentation(puzzle);
    }

    @Override
    public void update(float delta) {
        if (services == null || presentation == null) {
            return;
        }
        int[] size = screenSize();
        presentation.update(services.input(), size[0], size[1]);
    }

    @Override
    public void render() {
        if (services == null || presentation == null) {
            return;
        }
        int[] size = screenSize();
        presentation.render(services.draw2d(), font, size[0], size[1]);
    }

    @Override
    public void dispose() {
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (services != null) {
            services.draw2d().dispose();
        }
    }

    private int[] screenSize() {
        int w = services.graphics().getWidth();
        int h = services.graphics().getHeight();
        if (w <= 0) {
            w = 800;
        }
        if (h <= 0) {
            h = 600;
        }
        return new int[] {w, h};
    }
}
