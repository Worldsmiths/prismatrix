package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.core.Draw2D;
import io.github.worldsmiths.core.DrawMode;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.prismatrix.optics.BeamColor;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.puzzle.PlacementBoard;
import io.github.worldsmiths.prismatrix.sandbox.ObservedShot;
import java.util.Collections;
import java.util.List;

/**
 * Draw2D chrome for PvP vs AI: Classic orb+mirror tray, Ready / Fire / Guess, firer HUD + shot
 * history. Not Puzzle {@code ChromeOverlay} — Ghost badge chrome is out of scope.
 */
public final class PvpChrome {

    public enum ChromeHit {
        TRAY_ORB_R,
        TRAY_ORB_G,
        TRAY_ORB_B,
        TRAY_FORWARD,
        TRAY_BACK,
        READY,
        FIRE,
        GUESS,
        NONE
    }

    /** Relative to the first-ship chrome sizes (~1.35×). */
    static final float UI_SCALE = 1.35f;
    static final float TRAY_BTN = 44f * UI_SCALE;
    static final float TRAY_GAP = 8f * UI_SCALE;
    static final float READY_W = 100f * UI_SCALE;
    static final float READY_H = 36f * UI_SCALE;
    static final float ACTION_W = 88f * UI_SCALE;
    static final float ACTION_H = 36f * UI_SCALE;
    static final float ACTION_GAP = 10f * UI_SCALE;
    static final float MARGIN = 16f;
    static final float LINE_STEP = 26f * UI_SCALE;
    static final int DEFAULT_W = 800;
    static final int DEFAULT_H = 600;
    static final int HISTORY_MAX = 6;

    private PvpChrome() {}

    public static final class Layout {
        public final int screenWidth;
        public final int screenHeight;
        public final float trayLeft;
        public final float trayTop;
        public final float readyTop;
        public final float actionTop;
        public final float fireLeft;
        public final float guessLeft;
        public final float hudLeft;
        public final float hudTop;

        Layout(
                int screenWidth,
                int screenHeight,
                float trayLeft,
                float trayTop,
                float readyTop,
                float actionTop,
                float fireLeft,
                float guessLeft,
                float hudLeft,
                float hudTop) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.trayLeft = trayLeft;
            this.trayTop = trayTop;
            this.readyTop = readyTop;
            this.actionTop = actionTop;
            this.fireLeft = fireLeft;
            this.guessLeft = guessLeft;
            this.hudLeft = hudLeft;
            this.hudTop = hudTop;
        }

        public static Layout forSize(int screenWidth, int screenHeight) {
            int w = screenWidth > 0 ? screenWidth : DEFAULT_W;
            int h = screenHeight > 0 ? screenHeight : DEFAULT_H;
            float trayLeft = MARGIN;
            float trayTop = h - MARGIN - TRAY_BTN;
            float readyTop = trayTop - 12f - READY_H;
            float actionTop = readyTop;
            float fireLeft = trayLeft + READY_W + ACTION_GAP;
            float guessLeft = fireLeft + ACTION_W + ACTION_GAP;
            float hudLeft = Math.max(MARGIN, w - MARGIN - 360f * UI_SCALE);
            float hudTop = MARGIN;
            return new Layout(
                    w, h, trayLeft, trayTop, readyTop, actionTop, fireLeft, guessLeft, hudLeft, hudTop);
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

        public int fireCenterX() {
            return (int) (fireLeft + ACTION_W / 2f);
        }

        public int fireCenterY() {
            return (int) (actionTop + ACTION_H / 2f);
        }

        public int guessCenterX() {
            return (int) (guessLeft + ACTION_W / 2f);
        }

        public int guessCenterY() {
            return (int) (actionTop + ACTION_H / 2f);
        }
    }

    public static ChromeHit hitTest(int inputX, int inputY, int screenWidth, int screenHeight) {
        Layout layout = Layout.forSize(screenWidth, screenHeight);
        if (inRect(inputX, inputY, layout.fireLeft, layout.actionTop, ACTION_W, ACTION_H)) {
            return ChromeHit.FIRE;
        }
        if (inRect(inputX, inputY, layout.guessLeft, layout.actionTop, ACTION_W, ACTION_H)) {
            return ChromeHit.GUESS;
        }
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
            List<ObservedShot> fireLog,
            Port practicePort,
            boolean inputLocked,
            boolean canReady,
            boolean canFire,
            boolean canGuess) {
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
        if (phase == PvpPresentation.UiPhase.OWN_TURN) {
            drawAction(draw2d, layout, layout.fireLeft, layout.actionTop, canFire, 0.35f, 0.75f, 0.95f);
            drawAction(draw2d, layout, layout.guessLeft, layout.actionTop, canGuess, 0.85f, 0.55f, 0.25f);
        }
        if (font != null) {
            drawHud(
                    font,
                    draw2d,
                    layout,
                    phase,
                    trayBoard,
                    firerHud,
                    fireLog,
                    practicePort,
                    inputLocked,
                    canFire,
                    canGuess);
        }
    }

    private static void drawTray(Draw2D g, Layout layout, PieceType selected) {
        float trayDrawY = layout.screenHeight - layout.trayTop - TRAY_BTN;
        drawOrbSlot(g, layout.traySlotLeft(0), trayDrawY, selected == PieceType.ORB_R, 0.9f, 0.25f, 0.25f);
        drawOrbSlot(g, layout.traySlotLeft(1), trayDrawY, selected == PieceType.ORB_G, 0.25f, 0.85f, 0.3f);
        drawOrbSlot(g, layout.traySlotLeft(2), trayDrawY, selected == PieceType.ORB_B, 0.3f, 0.5f, 0.95f);
        g.begin(DrawMode.LINE);
        drawTraySlotBorder(
                g, layout.traySlotLeft(3), trayDrawY, selected == PieceType.FORWARD, 0.95f, 0.85f, 0.35f);
        drawSlash(g, layout.traySlotLeft(3), trayDrawY, TRAY_BTN, true);
        drawTraySlotBorder(
                g, layout.traySlotLeft(4), trayDrawY, selected == PieceType.BACK, 0.95f, 0.85f, 0.35f);
        drawSlash(g, layout.traySlotLeft(4), trayDrawY, TRAY_BTN, false);
        g.end();
    }

    private static void drawOrbSlot(
            Draw2D g, float left, float bottom, boolean selected, float r, float gr, float b) {
        g.begin(DrawMode.LINE);
        drawTraySlotBorder(g, left, bottom, selected, r, gr, b);
        g.end();
        float cx = left + TRAY_BTN * 0.5f;
        float cy = bottom + TRAY_BTN * 0.5f;
        float radius = TRAY_BTN * 0.32f;
        g.begin(DrawMode.FILLED);
        g.setColor(r, gr, b, 1f);
        fillCircle(g, cx, cy, radius);
        g.end();
    }

    private static void fillCircle(Draw2D g, float cx, float cy, float radius) {
        int r = Math.max(2, (int) Math.ceil(radius));
        for (int dy = -r; dy <= r; dy++) {
            float halfSq = radius * radius - dy * dy;
            if (halfSq < 0f) {
                continue;
            }
            float half = (float) Math.sqrt(halfSq);
            g.rect(cx - half, cy + dy, half * 2f, 1f);
        }
    }

    private static void drawTraySlotBorder(
            Draw2D g, float left, float bottom, boolean selected, float r, float gr, float b) {
        g.setColor(selected ? r : 0.55f, selected ? gr : 0.55f, selected ? b : 0.6f, 1f);
        g.rect(left, bottom, TRAY_BTN, TRAY_BTN);
    }

    private static void drawReady(Draw2D g, Layout layout, boolean canReady) {
        float y = layout.screenHeight - layout.readyTop - READY_H;
        g.begin(DrawMode.FILLED);
        g.setColor(canReady ? 0.2f : 0.15f, canReady ? 0.45f : 0.2f, canReady ? 0.25f : 0.22f, 0.85f);
        g.rect(layout.trayLeft, y, READY_W, READY_H);
        g.end();
        g.begin(DrawMode.LINE);
        g.setColor(canReady ? 0.35f : 0.45f, canReady ? 0.85f : 0.45f, canReady ? 0.4f : 0.5f, 1f);
        g.rect(layout.trayLeft, y, READY_W, READY_H);
        g.end();
    }

    private static void drawAction(
            Draw2D g, Layout layout, float left, float top, boolean enabled, float r, float gr, float b) {
        float y = layout.screenHeight - top - ACTION_H;
        g.begin(DrawMode.FILLED);
        g.setColor(
                enabled ? r * 0.35f : 0.18f,
                enabled ? gr * 0.35f : 0.18f,
                enabled ? b * 0.35f : 0.2f,
                0.9f);
        g.rect(left, y, ACTION_W, ACTION_H);
        g.end();
        g.begin(DrawMode.LINE);
        g.setColor(enabled ? r : 0.45f, enabled ? gr : 0.45f, enabled ? b : 0.5f, 1f);
        g.rect(left, y, ACTION_W, ACTION_H);
        g.end();
    }

    private static void drawHud(
            Font font,
            Draw2D draw2d,
            Layout layout,
            PvpPresentation.UiPhase phase,
            PlacementBoard trayBoard,
            FireObservation firerHud,
            List<ObservedShot> fireLog,
            Port practicePort,
            boolean inputLocked,
            boolean canFire,
            boolean canGuess) {
        float line = layout.screenHeight - layout.hudTop - 18f * UI_SCALE;
        drawText(font, "PvP: " + phase.name(), layout.hudLeft, line);
        line -= LINE_STEP;
        if (phase == PvpPresentation.UiPhase.FIRE_RESULT) {
            drawText(font, "Your Fire — tap / Enter / Space", layout.hudLeft, line);
            line -= LINE_STEP;
        } else if (phase == PvpPresentation.UiPhase.INCOMING_FIRE) {
            drawText(font, "Incoming Fire — tap / Enter / Space", layout.hudLeft, line);
            line -= LINE_STEP;
        } else if (inputLocked) {
            drawText(font, "Input locked (AI)", layout.hudLeft, line);
            line -= LINE_STEP;
        }
        if (phase == PvpPresentation.UiPhase.SETUP && trayBoard != null) {
            drawText(
                    font,
                    "Orbs: "
                            + trayBoard.remainingOrbs()
                            + "  Mirrors: "
                            + trayBoard.remainingMirrors(),
                    layout.hudLeft,
                    line);
            line -= LINE_STEP;
        }
        if (phase == PvpPresentation.UiPhase.OWN_TURN) {
            drawText(font, "Port tap = practice on guess board", layout.hudLeft, line);
            line -= LINE_STEP;
            if (practicePort != null) {
                drawText(font, "Practice port: " + practicePort, layout.hudLeft, line);
                line -= LINE_STEP;
            }
            drawText(
                    font,
                    "Fire btn"
                            + (canFire ? "" : " (pick port)")
                            + "   Guess btn"
                            + (canGuess ? "" : " (need 3+4)"),
                    layout.hudLeft,
                    line);
            line -= LINE_STEP;
            // Button labels (Draw2D coords: bottom-left).
            float fireY = layout.screenHeight - layout.actionTop - ACTION_H + 10f * UI_SCALE;
            float guessY = fireY;
            drawText(font, "FIRE", layout.fireLeft + 18f * UI_SCALE, fireY);
            drawText(font, "GUESS", layout.guessLeft + 10f * UI_SCALE, guessY);
        }
        if (phase == PvpPresentation.UiPhase.SETUP) {
            float readyY = layout.screenHeight - layout.readyTop - READY_H + 10f * UI_SCALE;
            drawText(font, "READY", layout.trayLeft + 18f * UI_SCALE, readyY);
        }
        if (firerHud != null) {
            Port exit = firerHud.exitPort();
            String exitLabel = exit == null ? "ABSORB" : exit.toString();
            String colorName = BeamColor.label(firerHud.colorMask());
            drawText(
                    font,
                    "Fire: " + firerHud.entryPort() + " -> " + exitLabel + "  " + colorName,
                    layout.hudLeft,
                    line);
            drawColorSwatch(
                    draw2d,
                    layout.hudLeft - 22f * UI_SCALE,
                    line - 4f,
                    16f * UI_SCALE,
                    firerHud.colorMask());
            line -= LINE_STEP;
            if (phase == PvpPresentation.UiPhase.FIRE_RESULT) {
                drawText(font, "Ports: entry white, exit " + colorName, layout.hudLeft, line);
                line -= LINE_STEP;
            }
        }
        List<ObservedShot> log = fireLog == null ? Collections.<ObservedShot>emptyList() : fireLog;
        if (!log.isEmpty()) {
            drawText(font, "Shot history (newest last):", layout.hudLeft, line);
            line -= LINE_STEP;
            int start = Math.max(0, log.size() - HISTORY_MAX);
            for (int i = start; i < log.size(); i++) {
                ObservedShot shot = log.get(i);
                Port exit = shot.exitPort();
                String exitLabel = exit == null ? "ABSORB" : exit.toString();
                String colorName = BeamColor.label(shot.colorMask());
                drawText(
                        font,
                        (i + 1)
                                + ". "
                                + shot.entryPort()
                                + "->"
                                + exitLabel
                                + " "
                                + colorName,
                        layout.hudLeft,
                        line);
                drawColorSwatch(
                        draw2d,
                        layout.hudLeft - 22f * UI_SCALE,
                        line - 4f,
                        14f * UI_SCALE,
                        shot.colorMask());
                line -= LINE_STEP * 0.9f;
            }
        }
    }

    private static void drawColorSwatch(Draw2D g, float left, float bottom, float size, int mask) {
        if (g == null) {
            return;
        }
        float[] c = BeamColor.rgba(mask);
        g.begin(DrawMode.FILLED);
        g.setColor(c[0], c[1], c[2], 1f);
        g.rect(left, bottom, size, size);
        g.end();
        g.begin(DrawMode.LINE);
        g.setColor(0.9f, 0.9f, 0.95f, 1f);
        g.rect(left, bottom, size, size);
        g.end();
    }

    /** Larger/heavier look without a Font scale API: double-draw + larger line step. */
    private static void drawText(Font font, String text, float x, float y) {
        font.drawString(text, x + 1f, y);
        font.drawString(text, x, y);
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
