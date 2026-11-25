package io.github.worldsmiths.prismatrix;

import io.github.worldsmiths.core.EngineException;
import io.github.worldsmiths.core.EngineServices;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.core.Input;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.Scene;
import io.github.worldsmiths.core.WorldsmithsGame;
import io.github.worldsmiths.core.ui.UiBounds;
import io.github.worldsmiths.core.ui.UiHit;
import io.github.worldsmiths.prismatrix.menu.AppSettings;
import io.github.worldsmiths.prismatrix.menu.MenuHub;
import io.github.worldsmiths.prismatrix.menu.MenuPhase;
import io.github.worldsmiths.prismatrix.menu.MenuUiController;
import io.github.worldsmiths.prismatrix.menu.WorldSelect;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import io.github.worldsmiths.prismatrix.puzzle.LevelLoadException;
import io.github.worldsmiths.prismatrix.puzzle.Presentation;
import io.github.worldsmiths.prismatrix.puzzle.PuzzleMode;
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
    private MenuUiController menuUi;
    private PlayMode playMode = PlayMode.MENU;
    private MenuPhase menuPhase = MenuPhase.HUB;

    private PuzzleMode puzzle;
    private Presentation presentation;

    private MatchKernel pvpKernel;
    private PvpPresentation pvpPresentation;
    private boolean oneKeyWasDown;
    private boolean twoKeyWasDown;
    private boolean threeKeyWasDown;
    private boolean escapeKeyWasDown;
    private boolean tabKeyWasDown;
    private boolean enterKeyWasDown;
    private boolean spaceKeyWasDown;
    /** Menu-selected orb palette for the next PvP match (default RYB easy). */
    private OrbColorPalette.Id menuOrbColors = OrbColorPalette.Id.RED_YELLOW_BLUE;
    /** Last AI tier chosen at {@link #enterPvp(boolean)}; false = Novice. */
    private boolean lastPvpTactician;

    PlayMode playMode() {
        return playMode;
    }

    MenuPhase menuPhase() {
        return menuPhase;
    }

    OrbColorPalette.Id menuOrbColors() {
        return menuOrbColors;
    }

    boolean lastPvpTactician() {
        return lastPvpTactician;
    }

    /** Package-visible so tests can hit tray centers without editing FakeUiKit. */
    UiBounds menuButtonBounds(String leafId) {
        return menuUi.buttonBounds(leafId);
    }

    /** Package-visible for Esc/play path tests until World Select / Match Start exist. */
    void enterPuzzleForTest() {
        enterPuzzle();
    }

    /** Package-visible for Esc/play path tests until Match Start exists. */
    void enterPvpForTest(boolean tactician) {
        enterPvp(tactician);
    }

    /** Package-visible for tests and menu navigation; clears kit and rebuilds phase. */
    void goToMenuPhase(MenuPhase phase) {
        if (phase == null) {
            throw new NullPointerException("phase");
        }
        menuPhase = phase;
        playMode = PlayMode.MENU;
        if (menuUi != null) {
            int[] size = screenSize();
            menuUi.setViewport(size[0], size[1]);
            menuUi.rebuild(phase);
        }
    }

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
        menuUi = new MenuUiController(services.uiKit());
        goToMenuPhase(MenuPhase.HUB);
    }

    @Override
    public void update(float delta) {
        if (services == null) {
            return;
        }
        Input input = services.input();
        PlayMode modeAtFrameStart = playMode;
        handleModeKeys(input);
        if (playMode == PlayMode.MENU) {
            handleMenuFocusKeys(input);
            handleMenuPointer(input);
        }
        int[] size = screenSize();
        // Skip play update on the entry frame so the menu tap does not hit the board.
        if (modeAtFrameStart == PlayMode.PUZZLE
                && playMode == PlayMode.PUZZLE
                && presentation != null) {
            presentation.update(input, size[0], size[1]);
        } else if (modeAtFrameStart == PlayMode.PVP
                && playMode == PlayMode.PVP
                && pvpPresentation != null) {
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
        if (menuUi != null) {
            menuUi.clear();
            menuUi = null;
        }
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
        boolean hubAccelerators =
                playMode == PlayMode.MENU && menuPhase == MenuPhase.HUB;

        boolean one = input.isKeyDown(Key.NUM_1);
        if (one && !oneKeyWasDown && hubAccelerators) {
            goToMenuPhase(MenuPhase.WORLD_SELECT);
        }
        oneKeyWasDown = one;

        boolean two = input.isKeyDown(Key.NUM_2);
        if (two && !twoKeyWasDown && hubAccelerators) {
            goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        }
        twoKeyWasDown = two;

        boolean three = input.isKeyDown(Key.NUM_3);
        if (three && !threeKeyWasDown && hubAccelerators) {
            goToMenuPhase(MenuPhase.APP_SETTINGS);
        }
        threeKeyWasDown = three;

        boolean esc = input.isKeyDown(Key.ESCAPE);
        if (esc && !escapeKeyWasDown) {
            if (playMode != PlayMode.MENU) {
                disposeActiveMode();
                goToMenuPhase(MenuPhase.HUB);
            } else if (menuPhase != MenuPhase.HUB) {
                goToMenuPhase(MenuPhase.HUB);
            }
        }
        escapeKeyWasDown = esc;
    }

    private void enterPuzzle() {
        disposeActiveMode();
        clearMenuKit();
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
        clearMenuKit();
        lastPvpTactician = tactician;
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

    private void clearMenuKit() {
        if (menuUi != null) {
            menuUi.clear();
        }
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

    private void handleMenuFocusKeys(Input input) {
        if (menuUi == null) {
            return;
        }
        boolean tab = input.isKeyDown(Key.TAB);
        if (tab && !tabKeyWasDown) {
            menuUi.focusNext();
        }
        tabKeyWasDown = tab;

        boolean enter = input.isKeyDown(Key.ENTER);
        boolean space = input.isKeyDown(Key.SPACE);
        if ((enter && !enterKeyWasDown) || (space && !spaceKeyWasDown)) {
            String id = menuUi.activateFocusedId();
            if (id != null) {
                dispatchMenuId(id);
            }
        }
        enterKeyWasDown = enter;
        spaceKeyWasDown = space;
    }

    private void handleMenuPointer(Input input) {
        if (menuUi == null || !input.justTouched()) {
            return;
        }
        UiHit hit = menuUi.hit(input.getX(), input.getY());
        if (hit.activated() && hit.id() != null) {
            dispatchMenuId(hit.id());
        }
    }

    private void dispatchMenuId(String id) {
        if (MenuHub.ID_PUZZLE.equals(id)) {
            goToMenuPhase(MenuPhase.WORLD_SELECT);
        } else if (MenuHub.ID_PVP.equals(id)) {
            goToMenuPhase(MenuPhase.MATCH_SETTINGS);
        } else if (MenuHub.ID_SETTINGS.equals(id)) {
            goToMenuPhase(MenuPhase.APP_SETTINGS);
        } else if (WorldSelect.ID_WORLD_1.equals(id)) {
            enterPuzzle();
        } else if (WorldSelect.ID_BACK.equals(id) || AppSettings.ID_BACK.equals(id)) {
            goToMenuPhase(MenuPhase.HUB);
        }
    }

    private void renderMenu(int screenWidth, int screenHeight) {
        if (menuUi == null || font == null) {
            return;
        }
        menuUi.setViewport(screenWidth, screenHeight);
        menuUi.draw(services.draw2d(), font);
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
