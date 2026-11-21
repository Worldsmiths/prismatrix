package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.core.Draw2D;
import io.github.worldsmiths.core.DrawMode;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.puzzle.PlacementBoard;

/**
 * Draw2D chrome for PvP vs AI: Classic orb+mirror tray, Ready, firer HUD (exit+color only). Not
 * Puzzle {@code ChromeOverlay} — Ghost badge chrome is out of scope.
 */
public final class PvpChrome {

    public enum ChromeHit {
        TRAY_ORB_R,
        TRAY_ORB_G,
        TRAY_ORB_B,
        TRAY_FORWARD,
        TRAY_BACK,
        READY,
        NONE
    }

    static final float TRAY_BTN = 44f;
    static final float TRAY_GAP = 8f;
    static final float READY_W = 100f;
    static final float READY_H = 36f;
    static final float MARGIN = 16f;
    static final int DEFAULT_W = 800;
    static final int DEFAULT_H = 600;

    private PvpChrome() {}

    public static final class Layout {
        public final int screenWidth;
        public final int screenHeight;
        public final float trayLeft;
        public final float trayTop;
        public final float readyTop;
        public final float hudLeft;
        public final float hudTop;

        Layout(
                int screenWidth,
                int screenHeight,
                float trayLeft,
                float trayTop,
                float readyTop,
                float hudLeft,
                float hudTop) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.trayLeft = trayLeft;
            this.trayTop = trayTop;
            this.readyTop = readyTop;
            this.hudLeft = hudLeft;
            this.hudTop = hudTop;
        }

        public static Layout forSize(int screenWidth, int screenHeight) {
            int w = screenWidth > 0 ? screenWidth : DEFAULT_W;
            int h = screenHeight > 0 ? screenHeight : DEFAULT_H;
            float trayLeft = MARGIN;
            float trayTop = h - MARGIN - TRAY_BTN;
            float readyTop = trayTop - 12f - READY_H;
            float hudLeft = Math.max(MARGIN, w - MARGIN - 300f);
            float hudTop = MARGIN;
            return new Layout(w, h, trayLeft, trayTop, readyTop, hudLeft, hudTop);
        }

        float traySlotLeft(int index) {
            return trayLeft + index * (TRAY_BTN + TRAY_GAP);
        }

        public int trayCenterY() {
            return (int) (trayTop + TRAY_BTN / 2f);
        }

        public int trayOrbRCenterX() {
            return (int) (traySlotLeft(0) + TRAY_BTN / 2f);
        }

        public int trayOrbGCenterX() {
            return (int) (traySlotLeft(1) + TRAY_BTN / 2f);
        }

        public int trayOrbBCenterX() {
            return (int) (traySlotLeft(2) + TRAY_BTN / 2f);
        }

        public int trayForwardCenterX() {
            return (int) (traySlotLeft(3) + TRAY_BTN / 2f);
        }

        public int trayBackCenterX() {
            return (int) (traySlotLeft(4) + TRAY_BTN / 2f);
        }

        public int readyCenterX() {
            return (int) (trayLeft + READY_W / 2f);
        }

        public int readyCenterY() {
            return (int) (readyTop + READY_H / 2f);
        }
    }

    public static ChromeHit hitTest(int inputX, int inputY, int screenWidth, int screenHeight) {
        Layout layout = Layout.forSize(screenWidth, screenHeight);
        if (inRect(inputX, inputY, layout.traySlotLeft(0), layout.trayTop, TRAY_BTN, TRAY_BTN)) {
            return ChromeHit.TRAY_ORB_R;
        }
        if (inRect(inputX, inputY, layout.traySlotLeft(1), layout.trayTop, TRAY_BTN, TRAY_BTN)) {
            return ChromeHit.TRAY_ORB_G;
        }
        if (inRect(inputX, inputY, layout.traySlotLeft(2), layout.trayTop, TRAY_BTN, TRAY_BTN)) {
            return ChromeHit.TRAY_ORB_B;
        }
        if (inRect(inputX, inputY, layout.traySlotLeft(3), layout.trayTop, TRAY_BTN, TRAY_BTN)) {
            return ChromeHit.TRAY_FORWARD;
        }
        if (inRect(inputX, inputY, layout.traySlotLeft(4), layout.trayTop, TRAY_BTN, TRAY_BTN)) {
            return ChromeHit.TRAY_BACK;
        }
        if (inRect(inputX, inputY, layout.trayLeft, layout.readyTop, READY_W, READY_H)) {
            return ChromeHit.READY;
        }
        return ChromeHit.NONE;
    }

    public static void render(
            Draw2D draw2d,
            Font font,
            int screenWidth,
            int screenHeight,
            PvpPresentation.UiPhase phase,
            PieceType selected,
            PlacementBoard trayBoard,
            FireObservation firerHud,
            boolean inputLocked,
            boolean canReady) {
        if (draw2d == null) {
            return;
        }
        Layout layout = Layout.forSize(screenWidth, screenHeight);
        draw2d.setOrtho2D(0f, 0f, layout.screenWidth, layout.screenHeight);
        if (phase == PvpPresentation.UiPhase.SETUP || phase == PvpPresentation.UiPhase.OWN_TURN) {
            drawTray(draw2d, layout, selected);
        }
        if (phase == PvpPresentation.UiPhase.SETUP) {
            drawReady(draw2d, layout, canReady);
        }
        if (font != null) {
            drawHud(font, draw2d, layout, phase, trayBoard, firerHud, inputLocked);
        }
    }

    private static void drawTray(Draw2D g, Layout layout, PieceType selected) {
        float trayDrawY = layout.screenHeight - layout.trayTop - TRAY_BTN;
        g.begin(DrawMode.LINE);
        drawTraySlot(g, layout.traySlotLeft(0), trayDrawY, selected == PieceType.ORB_R, 0.9f, 0.25f, 0.25f);
        drawTraySlot(g, layout.traySlotLeft(1), trayDrawY, selected == PieceType.ORB_G, 0.25f, 0.85f, 0.3f);
        drawTraySlot(g, layout.traySlotLeft(2), trayDrawY, selected == PieceType.ORB_B, 0.3f, 0.5f, 0.95f);
        drawTraySlot(g, layout.traySlotLeft(3), trayDrawY, selected == PieceType.FORWARD, 0.95f, 0.85f, 0.35f);
        drawSlash(g, layout.traySlotLeft(3), trayDrawY, TRAY_BTN, true);
        drawTraySlot(g, layout.traySlotLeft(4), trayDrawY, selected == PieceType.BACK, 0.95f, 0.85f, 0.35f);
        drawSlash(g, layout.traySlotLeft(4), trayDrawY, TRAY_BTN, false);
        g.end();
    }

    private static void drawTraySlot(
            Draw2D g, float left, float bottom, boolean selected, float r, float gr, float b) {
        g.setColor(selected ? r : 0.55f, selected ? gr : 0.55f, selected ? b : 0.6f, 1f);
        g.rect(left, bottom, TRAY_BTN, TRAY_BTN);
    }

    private static void drawReady(Draw2D g, Layout layout, boolean canReady) {
        float y = layout.screenHeight - layout.readyTop - READY_H;
        g.begin(DrawMode.LINE);
        g.setColor(canReady ? 0.35f : 0.45f, canReady ? 0.85f : 0.45f, canReady ? 0.4f : 0.5f, 1f);
        g.rect(layout.trayLeft, y, READY_W, READY_H);
        g.end();
    }

    private static void drawHud(
            Font font,
            Draw2D draw2d,
            Layout layout,
            PvpPresentation.UiPhase phase,
            PlacementBoard trayBoard,
            FireObservation firerHud,
            boolean inputLocked) {
        float line = layout.screenHeight - layout.hudTop - 16f;
        font.drawString("PvP: " + phase.name(), layout.hudLeft, line);
        line -= 26f;
        if (phase == PvpPresentation.UiPhase.FIRE_RESULT) {
            font.drawString("Your Fire result — tap / Enter / Space to continue", layout.hudLeft, line);
            line -= 26f;
        } else if (phase == PvpPresentation.UiPhase.INCOMING_FIRE) {
            font.drawString("Incoming Fire on your board — tap / Enter / Space", layout.hudLeft, line);
            line -= 26f;
        } else if (inputLocked) {
            font.drawString("Input locked (AI)", layout.hudLeft, line);
            line -= 26f;
        }
        if (phase == PvpPresentation.UiPhase.SETUP && trayBoard != null) {
            font.drawString(
                    "Orbs left: "
                            + trayBoard.remainingOrbs()
                            + "  Mirrors: "
                            + trayBoard.remainingMirrors(),
                    layout.hudLeft,
                    line);
            line -= 26f;
        }
        // Firer HUD: exit + color only — never path cells.
        if (firerHud != null) {
            Port exit = firerHud.exitPort();
            String exitLabel = exit == null ? "ABSORB" : exit.toString();
            font.drawString(
                    "Fire: "
                            + firerHud.entryPort()
                            + " -> "
                            + exitLabel
                            + "  color="
                            + firerHud.colorMask(),
                    layout.hudLeft,
                    line);
        }
    }

    private static void drawSlash(
            Draw2D g, float cellLeft, float cellBottom, float size, boolean forward) {
        float pad = size * 0.2f;
        if (forward) {
            g.line(cellLeft + pad, cellBottom + pad, cellLeft + size - pad, cellBottom + size - pad);
        } else {
            g.line(cellLeft + pad, cellBottom + size - pad, cellLeft + size - pad, cellBottom + pad);
        }
    }

    private static boolean inRect(
            int x, int y, float left, float top, float width, float height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
