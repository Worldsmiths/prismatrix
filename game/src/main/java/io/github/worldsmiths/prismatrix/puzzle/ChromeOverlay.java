package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.Draw2D;
import io.github.worldsmiths.core.DrawMode;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.ArrayList;
import java.util.List;

/**
 * Draw2D / Font chrome: mirror + dynamic orb tray, remaining budget, compact goals, Verify. Layout
 * is recomputed from screen size so resize keeps controls in the left/bottom and HUD in the
 * top-right margins around the pitched board.
 */
public final class ChromeOverlay {

    public enum ChromeHit {
        TRAY_FORWARD,
        TRAY_BACK,
        TRAY_ORB_1,
        TRAY_ORB_2,
        TRAY_ORB_3,
        NONE
    }

    static final float TRAY_BTN = 48f;
    static final float TRAY_GAP = 12f;
    static final float MARGIN = 16f;
    /** Default headed / Fake size used by convenience hit centers in tests. */
    static final int DEFAULT_W = 800;
    static final int DEFAULT_H = 600;

    private ChromeOverlay() {}

    /** Screen-relative chrome positions (Input space: origin top-left, Y down). */
    public static final class Layout {
        public final int screenWidth;
        public final int screenHeight;
        public final float trayLeft;
        public final float trayTop;
        public final float hudLeft;
        public final float hudTop;
        /** Tray slots in draw order: FORWARD, BACK, then visible orbs. */
        private final List<ChromeHit> traySlots;

        Layout(
                int screenWidth,
                int screenHeight,
                float trayLeft,
                float trayTop,
                float hudLeft,
                float hudTop,
                List<ChromeHit> traySlots) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.trayLeft = trayLeft;
            this.trayTop = trayTop;
            this.hudLeft = hudLeft;
            this.hudTop = hudTop;
            this.traySlots = traySlots;
        }

        public static Layout forSize(int screenWidth, int screenHeight) {
            return forPuzzle(screenWidth, screenHeight, null);
        }

        public static Layout forPuzzle(int screenWidth, int screenHeight, PuzzleMode puzzle) {
            int w = screenWidth > 0 ? screenWidth : DEFAULT_W;
            int h = screenHeight > 0 ? screenHeight : DEFAULT_H;
            float trayLeft = MARGIN;
            float trayTop = h - MARGIN - TRAY_BTN;
            float hudLeft = Math.max(MARGIN, w - MARGIN - 280f);
            float hudTop = MARGIN;
            return new Layout(w, h, trayLeft, trayTop, hudLeft, hudTop, buildTraySlots(puzzle));
        }

        private static List<ChromeHit> buildTraySlots(PuzzleMode puzzle) {
            List<ChromeHit> slots = new ArrayList<ChromeHit>(5);
            slots.add(ChromeHit.TRAY_FORWARD);
            slots.add(ChromeHit.TRAY_BACK);
            if (puzzle != null) {
                if (puzzle.level().orb1() > 0) {
                    slots.add(ChromeHit.TRAY_ORB_1);
                }
                if (puzzle.level().orb2() > 0) {
                    slots.add(ChromeHit.TRAY_ORB_2);
                }
                if (puzzle.level().orb3() > 0) {
                    slots.add(ChromeHit.TRAY_ORB_3);
                }
            }
            return slots;
        }

        float traySlotLeft(int index) {
            return trayLeft + index * (TRAY_BTN + TRAY_GAP);
        }

        private int traySlotIndex(ChromeHit hit) {
            for (int i = 0; i < traySlots.size(); i++) {
                if (traySlots.get(i) == hit) {
                    return i;
                }
            }
            return -1;
        }

        public int trayForwardCenterX() {
            return (int) (traySlotLeft(traySlotIndex(ChromeHit.TRAY_FORWARD)) + TRAY_BTN / 2f);
        }

        public int trayBackCenterX() {
            return (int) (traySlotLeft(traySlotIndex(ChromeHit.TRAY_BACK)) + TRAY_BTN / 2f);
        }

        public int trayOrbCenterX(int channel) {
            ChromeHit hit =
                    channel == 0
                            ? ChromeHit.TRAY_ORB_1
                            : channel == 1 ? ChromeHit.TRAY_ORB_2 : ChromeHit.TRAY_ORB_3;
            int index = traySlotIndex(hit);
            if (index < 0) {
                throw new IllegalArgumentException("orb channel " + channel + " not in tray");
            }
            return (int) (traySlotLeft(index) + TRAY_BTN / 2f);
        }

        public int trayCenterY() {
            return (int) (trayTop + TRAY_BTN / 2f);
        }
    }

    public static int trayForwardCenterX() {
        return Layout.forSize(DEFAULT_W, DEFAULT_H).trayForwardCenterX();
    }

    public static int trayBackCenterX() {
        return Layout.forSize(DEFAULT_W, DEFAULT_H).trayBackCenterX();
    }

    public static int trayCenterY() {
        return Layout.forSize(DEFAULT_W, DEFAULT_H).trayCenterY();
    }

    public static ChromeHit hitTest(int inputX, int inputY) {
        return hitTest(inputX, inputY, DEFAULT_W, DEFAULT_H, null);
    }

    public static ChromeHit hitTest(int inputX, int inputY, int screenWidth, int screenHeight) {
        return hitTest(inputX, inputY, screenWidth, screenHeight, null);
    }

    public static ChromeHit hitTest(
            int inputX, int inputY, int screenWidth, int screenHeight, PuzzleMode puzzle) {
        return hitTest(inputX, inputY, Layout.forPuzzle(screenWidth, screenHeight, puzzle));
    }

    public static ChromeHit hitTest(int inputX, int inputY, Layout layout) {
        for (int i = 0; i < layout.traySlots.size(); i++) {
            if (inRect(
                    inputX,
                    inputY,
                    layout.traySlotLeft(i),
                    layout.trayTop,
                    TRAY_BTN,
                    TRAY_BTN)) {
                return layout.traySlots.get(i);
            }
        }
        return ChromeHit.NONE;
    }

    public static void render(
            Draw2D draw2d,
            Font font,
            int screenWidth,
            int screenHeight,
            PieceType selected,
            PuzzleMode puzzle,
            VerifyResult lastVerify,
            OrbColorPalette palette) {
        if (draw2d == null) {
            return;
        }
        Layout layout = Layout.forPuzzle(screenWidth, screenHeight, puzzle);
        OrbColorPalette colors =
                palette == null ? OrbColorPalette.defaultPalette() : palette;
        draw2d.setOrtho2D(0f, 0f, layout.screenWidth, layout.screenHeight);
        drawTray(draw2d, layout, selected, colors);
        if (font != null) {
            drawHud(font, layout, puzzle, lastVerify);
        }
    }

    private static void drawTray(
            Draw2D g, Layout layout, PieceType selected, OrbColorPalette palette) {
        float trayDrawY = layout.screenHeight - layout.trayTop - TRAY_BTN;
        for (int i = 0; i < layout.traySlots.size(); i++) {
            ChromeHit slot = layout.traySlots.get(i);
            float left = layout.traySlotLeft(i);
            switch (slot) {
                case TRAY_FORWARD:
                    drawMirrorSlot(g, left, trayDrawY, selected == PieceType.FORWARD, true);
                    break;
                case TRAY_BACK:
                    drawMirrorSlot(g, left, trayDrawY, selected == PieceType.BACK, false);
                    break;
                case TRAY_ORB_1:
                    drawOrbSlot(g, left, trayDrawY, selected == PieceType.ORB_1, palette, 0);
                    break;
                case TRAY_ORB_2:
                    drawOrbSlot(g, left, trayDrawY, selected == PieceType.ORB_2, palette, 1);
                    break;
                case TRAY_ORB_3:
                    drawOrbSlot(g, left, trayDrawY, selected == PieceType.ORB_3, palette, 2);
                    break;
                default:
                    break;
            }
        }
    }

    private static void drawMirrorSlot(
            Draw2D g, float left, float trayDrawY, boolean selected, boolean forward) {
        g.begin(DrawMode.LINE);
        g.setColor(
                selected ? 0.95f : 0.7f,
                selected ? 0.85f : 0.7f,
                selected ? 0.35f : 0.72f,
                1f);
        g.rect(left, trayDrawY, TRAY_BTN, TRAY_BTN);
        drawSlash(g, left, trayDrawY, TRAY_BTN, forward);
        g.end();
    }

    private static void drawOrbSlot(
            Draw2D g,
            float left,
            float trayDrawY,
            boolean selected,
            OrbColorPalette palette,
            int channel) {
        float[] c = palette.rgbaForPiece(PieceType.orb(channel));
        g.begin(DrawMode.FILLED);
        g.setColor(c[0], c[1], c[2], selected ? 1f : 0.75f);
        float pad = TRAY_BTN * 0.18f;
        g.rect(left + pad, trayDrawY + pad, TRAY_BTN - 2f * pad, TRAY_BTN - 2f * pad);
        g.end();
        g.begin(DrawMode.LINE);
        g.setColor(
                selected ? 0.95f : 0.7f,
                selected ? 0.85f : 0.7f,
                selected ? 0.35f : 0.72f,
                1f);
        g.rect(left, trayDrawY, TRAY_BTN, TRAY_BTN);
        g.end();
    }

    private static void drawHud(
            Font font, Layout layout, PuzzleMode puzzle, VerifyResult lastVerify) {
        float line = layout.screenHeight - layout.hudTop - 16f;
        font.drawString(remainingChrome(puzzle), layout.hudLeft, line);
        line -= 28f;
        font.drawString("Goals:", layout.hudLeft, line);
        List<GoalDefinition> goals = puzzle.goals();
        for (int i = 0; i < goals.size(); i++) {
            line -= 22f;
            GoalDefinition g = goals.get(i);
            font.drawString(
                    g.entryPort() + " -> " + g.exitPort() + " mask=" + g.colorMask(),
                    layout.hudLeft,
                    line);
        }
        line -= 36f;
        font.drawString(verifyChrome(puzzle, lastVerify), layout.hudLeft, line);
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
            font.drawString(sb.toString(), layout.hudLeft, line);
        }
    }

    private static String remainingChrome(PuzzleMode puzzle) {
        StringBuilder sb = new StringBuilder();
        sb.append("Remaining: ")
                .append(puzzle.remainingMirrors())
                .append(" / ")
                .append(puzzle.mirrorBudget())
                .append(" mirrors");
        if (puzzle.level().orb1() > 0
                || puzzle.level().orb2() > 0
                || puzzle.level().orb3() > 0) {
            sb.append(" | orbs ")
                    .append(puzzle.remainingOrb(PieceType.ORB_1))
                    .append("/")
                    .append(puzzle.remainingOrb(PieceType.ORB_2))
                    .append("/")
                    .append(puzzle.remainingOrb(PieceType.ORB_3));
        }
        return sb.toString();
    }

    private static String verifyChrome(PuzzleMode puzzle, VerifyResult lastVerify) {
        if (!puzzle.canVerify()) {
            return "Verify: disabled (spend inventory)";
        }
        if (lastVerify == null) {
            return "Verify: ready (ENTER / V)";
        }
        if (lastVerify.isOk()) {
            return "Verify: OK";
        }
        if (lastVerify.isRefused()) {
            return "Verify: disabled (spend inventory)";
        }
        return "Verify: FAIL";
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
