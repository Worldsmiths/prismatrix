package io.github.worldsmiths.prismatrix;

import io.github.worldsmiths.core.EngineException;
import io.github.worldsmiths.core.EngineServices;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.core.Input;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.Scene;
import io.github.worldsmiths.core.WorldsmithsGame;
import io.github.worldsmiths.prismatrix.puzzle.LevelLoadException;
import io.github.worldsmiths.prismatrix.puzzle.Presentation;
import io.github.worldsmiths.prismatrix.puzzle.PuzzleMode;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import io.github.worldsmiths.prismatrix.pvp.MatchKernel;
import io.github.worldsmiths.prismatrix.pvp.MatchRules;
import io.github.worldsmiths.prismatrix.pvp.NoviceAiPolicy;
import io.github.worldsmiths.prismatrix.pvp.PvpPresentation;
import io.github.worldsmiths.prismatrix.pvp.SeatId;
import io.github.worldsmiths.prismatrix.pvp.TacticianAiPolicy;
import java.util.Random;

/**
 * Prismatrix desktop: mode entry for Puzzle W1 or PvP vs AI (Novice / Tactician). Puzzle keeps
 * {@link #FIRST_LEVEL_PATH} at {@code levels/w1/01.json}.
 */
public final class PrismatrixGame implements WorldsmithsGame {

    static final String FIRST_LEVEL_PATH = "levels/w1/01.json";
    static final String HUD_FONT = "fonts/hud.fnt";

    enum PlayMode {
        MENU,
        PUZZLE,
        PVP
    }

    private EngineServices services;
    private Font font;
    private PlayMode playMode = PlayMode.MENU;

    private PuzzleMode puzzle;
    private Presentation presentation;

    private MatchKernel pvpKernel;
    private PvpPresentation pvpPresentation;
    private boolean oneKeyWasDown;
    private boolean twoKeyWasDown;
    private boolean threeKeyWasDown;
    private boolean escapeKeyWasDown;
    private boolean cKeyWasDown;
    /** Menu-selected orb palette for the next PvP match (default RYB easy). */
    private OrbColorPalette.Id menuOrbColors = OrbColorPalette.Id.RED_YELLOW_BLUE;

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
        font = services.ui().createFont(HUD_FONT);
        playMode = PlayMode.MENU;
    }

    @Override
    public void update(float delta) {
        if (services == null) {
            return;
        }
        Input input = services.input();
        handleModeKeys(input);
        int[] size = screenSize();
        if (playMode == PlayMode.PUZZLE && presentation != null) {
            presentation.update(input, size[0], size[1]);
        } else if (playMode == PlayMode.PVP && pvpPresentation != null) {
            pvpPresentation.update(input, size[0], size[1]);
        }
    }

    @Override
    public void render() {
        if (services == null) {
            return;
        }
        int[] size = screenSize();
        if (playMode == PlayMode.MENU) {
            renderMenu(size[0], size[1]);
            return;
        }
        if (playMode == PlayMode.PUZZLE && presentation != null) {
            presentation.render(services.draw2d(), font, size[0], size[1]);
        } else if (playMode == PlayMode.PVP && pvpPresentation != null) {
            pvpPresentation.render(services.draw2d(), font, size[0], size[1]);
        }
    }

    @Override
    public void dispose() {
        disposeActiveMode();
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (services != null) {
            services.draw2d().dispose();
        }
    }

    private void handleModeKeys(Input input) {
        if (input == null) {
            return;
        }
        boolean one = input.isKeyDown(Key.NUM_1);
        if (one && !oneKeyWasDown && playMode == PlayMode.MENU) {
            enterPuzzle();
        }
        oneKeyWasDown = one;

        boolean two = input.isKeyDown(Key.NUM_2);
        if (two && !twoKeyWasDown && playMode == PlayMode.MENU) {
            enterPvp(false);
        }
        twoKeyWasDown = two;

        boolean three = input.isKeyDown(Key.NUM_3);
        if (three && !threeKeyWasDown && playMode == PlayMode.MENU) {
            enterPvp(true);
        }
        threeKeyWasDown = three;

        boolean esc = input.isKeyDown(Key.ESCAPE);
        if (esc && !escapeKeyWasDown && playMode != PlayMode.MENU) {
            disposeActiveMode();
            playMode = PlayMode.MENU;
        }
        escapeKeyWasDown = esc;

        boolean c = input.isKeyDown(Key.C);
        if (c && !cKeyWasDown && playMode == PlayMode.MENU) {
            menuOrbColors =
                    menuOrbColors == OrbColorPalette.Id.RED_YELLOW_BLUE
                            ? OrbColorPalette.Id.RED_GREEN_BLUE
                            : OrbColorPalette.Id.RED_YELLOW_BLUE;
        }
        cKeyWasDown = c;
    }

    private void enterPuzzle() {
        disposeActiveMode();
        try {
            puzzle = PuzzleMode.load(services.files(), FIRST_LEVEL_PATH);
        } catch (LevelLoadException e) {
            throw new EngineException("Failed to load " + FIRST_LEVEL_PATH, e);
        }
        presentation = new Presentation(puzzle, services.thin3d());
        playMode = PlayMode.PUZZLE;
    }

    private void enterPvp(boolean tactician) {
        disposeActiveMode();
        MatchRules rules = MatchRules.builder().orbColors(menuOrbColors).build();
        pvpKernel = new MatchKernel(rules, new Random());
        pvpPresentation =
                new PvpPresentation(
                        pvpKernel,
                        SeatId.A,
                        SeatId.B,
                        tactician ? new TacticianAiPolicy() : new NoviceAiPolicy(),
                        services.thin3d(),
                        new Random());
        playMode = PlayMode.PVP;
    }

    private void disposeActiveMode() {
        if (presentation != null) {
            presentation.dispose();
            presentation = null;
        }
        puzzle = null;
        if (pvpPresentation != null) {
            pvpPresentation.dispose();
            pvpPresentation = null;
        }
        pvpKernel = null;
    }

    private void renderMenu(int screenWidth, int screenHeight) {
        if (font == null || services.draw2d() == null) {
            return;
        }
        services.draw2d().setOrtho2D(0f, 0f, screenWidth, screenHeight);
        float y = screenHeight - 80f;
        font.drawString("Prismatrix", 40f, y);
        y -= 48f;
        font.drawString("1 — Puzzle", 40f, y);
        y -= 32f;
        font.drawString("2 — PvP vs AI (Novice)", 40f, y);
        y -= 32f;
        font.drawString("3 — PvP vs AI (Tactician)", 40f, y);
        y -= 32f;
        font.drawString(
                "C — colors: " + OrbColorPalette.of(menuOrbColors).shortLabel(), 40f, y);
        y -= 32f;
        font.drawString("Esc — menu", 40f, y);
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
