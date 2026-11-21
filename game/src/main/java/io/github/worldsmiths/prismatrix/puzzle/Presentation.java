package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.Draw2D;
import io.github.worldsmiths.core.DrawMode;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.core.Input;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.List;

/**
 * Flat P0 Draw2D + Font + Input presentation for Puzzle W1.
 *
 * <p>Y adapters: {@link Input} is top-left origin; {@link Draw2D} ortho is bottom-left. Optics
 * {@code y=0} is mapped to the visual top of the grid so T ports sit along the top edge.
 */
public final class Presentation {

    static final float CELL = 56f;
    static final float GRID_LEFT = 40f;
    /** Input-space top of the optics grid (smaller Y = higher on screen). */
    static final float GRID_TOP = 40f;
    static final float TRAY_TOP = 400f;
    static final float TRAY_BTN = 48f;
    static final float TRAY_GAP = 16f;
    static final float HUD_LEFT = 400f;

    private final PuzzleMode puzzle;
    private PieceType selected;
    private VerifyResult lastVerify;
    private boolean verifyKeyWasDown;

    public Presentation(PuzzleMode puzzle) {
        if (puzzle == null) {
            throw new IllegalArgumentException("puzzle must not be null");
        }
        this.puzzle = puzzle;
    }

    public void update(Input input, int screenWidth, int screenHeight) {
        if (input == null) {
            return;
        }
        if (input.justTouched()) {
            onTap(input.getX(), input.getY());
        }
        boolean verifyDown = input.isKeyDown(Key.ENTER) || input.isKeyDown(Key.V);
        if (verifyDown && !verifyKeyWasDown && puzzle.canVerify()) {
            lastVerify = puzzle.verify();
        }
        verifyKeyWasDown = verifyDown;
    }

    public void render(Draw2D draw2d, Font font, int screenWidth, int screenHeight) {
        if (draw2d == null) {
            return;
        }
        int w = screenWidth > 0 ? screenWidth : 800;
        int h = screenHeight > 0 ? screenHeight : 600;
        draw2d.setOrtho2D(0f, 0f, w, h);
        drawGrid(draw2d, h);
        drawTray(draw2d, h);
        drawMirrors(draw2d, h);
        if (font != null) {
            drawHud(font, h);
        }
    }

    /** Last Verify outcome from keyboard, or {@code null} if Verify has not run. */
    public VerifyResult lastVerifyResult() {
        return lastVerify;
    }

    /**
     * Hit-test in Input coordinates (top-left origin). Returns {@code {opticsX, opticsY}} or {@code
     * null} if outside the grid.
     */
    int[] hitTestCell(int inputX, int inputY) {
        int n = puzzle.gridSize();
        float gridRight = GRID_LEFT + n * CELL;
        float gridBottomInput = GRID_TOP + n * CELL;
        if (inputX < GRID_LEFT || inputX >= gridRight || inputY < GRID_TOP || inputY >= gridBottomInput) {
            return null;
        }
        int ox = (int) ((inputX - GRID_LEFT) / CELL);
        // Optics y=0 at visual top: smaller inputY → smaller opticsY.
        int oy = (int) ((inputY - GRID_TOP) / CELL);
        if (ox < 0 || ox >= n || oy < 0 || oy >= n) {
            return null;
        }
        return new int[] {ox, oy};
    }

    static int trayForwardCenterX() {
        return (int) (GRID_LEFT + TRAY_BTN / 2f);
    }

    static int trayBackCenterX() {
        return (int) (GRID_LEFT + TRAY_BTN + TRAY_GAP + TRAY_BTN / 2f);
    }

    static int trayCenterY() {
        return (int) (TRAY_TOP + TRAY_BTN / 2f);
    }

    private void onTap(int inputX, int inputY) {
        if (hitTrayForward(inputX, inputY)) {
            selected = PieceType.FORWARD;
            return;
        }
        if (hitTrayBack(inputX, inputY)) {
            selected = PieceType.BACK;
            return;
        }
        int[] cell = hitTestCell(inputX, inputY);
        if (cell == null) {
            return;
        }
        int ox = cell[0];
        int oy = cell[1];
        PieceType existing = puzzle.pieceAt(ox, oy);
        if (existing != null) {
            puzzle.remove(ox, oy);
            lastVerify = null;
            return;
        }
        if (selected != null) {
            PlaceResult result = puzzle.place(ox, oy, selected);
            if (result == PlaceResult.OK) {
                lastVerify = null;
            }
        }
    }

    private static boolean hitTrayForward(int inputX, int inputY) {
        return inRect(inputX, inputY, GRID_LEFT, TRAY_TOP, TRAY_BTN, TRAY_BTN);
    }

    private static boolean hitTrayBack(int inputX, int inputY) {
        return inRect(
                inputX, inputY, GRID_LEFT + TRAY_BTN + TRAY_GAP, TRAY_TOP, TRAY_BTN, TRAY_BTN);
    }

    private static boolean inRect(
            int x, int y, float left, float top, float width, float height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private void drawGrid(Draw2D g, int screenHeight) {
        int n = puzzle.gridSize();
        float gridBottom = screenHeight - GRID_TOP - n * CELL;
        g.begin(DrawMode.LINE);
        g.setColor(0.55f, 0.55f, 0.6f, 1f);
        for (int i = 0; i <= n; i++) {
            float x = GRID_LEFT + i * CELL;
            g.line(x, gridBottom, x, gridBottom + n * CELL);
            float y = gridBottom + i * CELL;
            g.line(GRID_LEFT, y, GRID_LEFT + n * CELL, y);
        }
        g.end();
    }

    private void drawTray(Draw2D g, int screenHeight) {
        float trayDrawY = screenHeight - TRAY_TOP - TRAY_BTN;
        g.begin(DrawMode.LINE);
        g.setColor(
                selected == PieceType.FORWARD ? 0.95f : 0.7f,
                selected == PieceType.FORWARD ? 0.85f : 0.7f,
                selected == PieceType.FORWARD ? 0.35f : 0.72f,
                1f);
        g.rect(GRID_LEFT, trayDrawY, TRAY_BTN, TRAY_BTN);
        drawSlash(g, GRID_LEFT, trayDrawY, TRAY_BTN, true);
        float backLeft = GRID_LEFT + TRAY_BTN + TRAY_GAP;
        g.setColor(
                selected == PieceType.BACK ? 0.95f : 0.7f,
                selected == PieceType.BACK ? 0.85f : 0.7f,
                selected == PieceType.BACK ? 0.35f : 0.72f,
                1f);
        g.rect(backLeft, trayDrawY, TRAY_BTN, TRAY_BTN);
        drawSlash(g, backLeft, trayDrawY, TRAY_BTN, false);
        g.end();
    }

    private void drawMirrors(Draw2D g, int screenHeight) {
        int n = puzzle.gridSize();
        float gridBottom = screenHeight - GRID_TOP - n * CELL;
        g.begin(DrawMode.LINE);
        g.setColor(0.9f, 0.9f, 0.95f, 1f);
        for (int oy = 0; oy < n; oy++) {
            for (int ox = 0; ox < n; ox++) {
                PieceType piece = puzzle.pieceAt(ox, oy);
                if (piece == null) {
                    continue;
                }
                float cellLeft = GRID_LEFT + ox * CELL;
                // Optics y=0 at visual top → highest Draw2D y among cells.
                float cellBottom = gridBottom + (n - 1 - oy) * CELL;
                drawSlash(g, cellLeft, cellBottom, CELL, piece == PieceType.FORWARD);
            }
        }
        g.end();
    }

    private static void drawSlash(
            Draw2D g, float cellLeft, float cellBottom, float size, boolean forward) {
        float pad = size * 0.2f;
        if (forward) {
            // FORWARD = / : bottom-left to top-right in Draw2D space
            g.line(cellLeft + pad, cellBottom + pad, cellLeft + size - pad, cellBottom + size - pad);
        } else {
            // BACK = \ : top-left to bottom-right in Draw2D space
            g.line(cellLeft + pad, cellBottom + size - pad, cellLeft + size - pad, cellBottom + pad);
        }
    }

    private void drawHud(Font font, int screenHeight) {
        float line = screenHeight - 48f;
        font.drawString("Remaining: " + puzzle.remaining() + " / " + puzzle.mirrorBudget(), HUD_LEFT, line);
        line -= 28f;
        font.drawString("Clues:", HUD_LEFT, line);
        List<ClueDefinition> clues = puzzle.clues();
        for (int i = 0; i < clues.size(); i++) {
            line -= 22f;
            ClueDefinition c = clues.get(i);
            font.drawString(
                    c.entryPort() + " -> " + c.exitPort() + " mask=" + c.colorMask(),
                    HUD_LEFT,
                    line);
        }
        line -= 36f;
        font.drawString(verifyChrome(), HUD_LEFT, line);
        line -= 24f;
        if (lastVerify != null && !lastVerify.isOk() && !lastVerify.isRefused()) {
            List<Port> ports = lastVerify.mismatchedEntryPorts();
            StringBuilder sb = new StringBuilder("Mismatch: ");
            for (int i = 0; i < ports.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(ports.get(i));
            }
            font.drawString(sb.toString(), HUD_LEFT, line);
        }
    }

    private String verifyChrome() {
        if (!puzzle.canVerify()) {
            return "Verify: disabled (spend budget)";
        }
        if (lastVerify == null) {
            return "Verify: ready (ENTER / V)";
        }
        if (lastVerify.isOk()) {
            return "Verify: OK";
        }
        if (lastVerify.isRefused()) {
            return "Verify: disabled (spend budget)";
        }
        return "Verify: FAIL";
    }
}
