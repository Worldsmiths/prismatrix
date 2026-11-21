package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.Draw2D;
import io.github.worldsmiths.core.DrawMode;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.sandbox.ConsistencyBadge;
import io.github.worldsmiths.prismatrix.sandbox.PromoteResult;
import java.util.Collections;
import java.util.List;

/**
 * Draw2D / Font chrome: Ghost|Draft mode chip, tray, remaining, badges as text+color markers,
 * Promote / Verify refuse chrome. Layout is recomputed from screen size so resize keeps controls
 * in the left/bottom and HUD in the top-right margins around the pitched board.
 */
public final class ChromeOverlay {

    public enum EditMode {
        GHOST,
        DRAFT
    }

    public enum ChromeHit {
        TRAY_FORWARD,
        TRAY_BACK,
        MODE_GHOST,
        MODE_DRAFT,
        PROMOTE,
        NONE
    }

    static final float TRAY_BTN = 48f;
    static final float TRAY_GAP = 12f;
    static final float MODE_BTN_W = 72f;
    static final float PROMOTE_W = 100f;
    static final float PROMOTE_H = 36f;
    static final float MARGIN = 16f;
    static final float STACK_GAP = 10f;
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
        public final float modeTop;
        public final float promoteTop;
        public final float hudLeft;
        public final float hudTop;

        Layout(
                int screenWidth,
                int screenHeight,
                float trayLeft,
                float trayTop,
                float modeTop,
                float promoteTop,
                float hudLeft,
                float hudTop) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.trayLeft = trayLeft;
            this.trayTop = trayTop;
            this.modeTop = modeTop;
            this.promoteTop = promoteTop;
            this.hudLeft = hudLeft;
            this.hudTop = hudTop;
        }

        public static Layout forSize(int screenWidth, int screenHeight) {
            int w = screenWidth > 0 ? screenWidth : DEFAULT_W;
            int h = screenHeight > 0 ? screenHeight : DEFAULT_H;
            float trayLeft = MARGIN;
            // Stack from the bottom edge: tray, then mode chips, then promote.
            float trayTop = h - MARGIN - TRAY_BTN;
            float modeTop = trayTop - STACK_GAP - TRAY_BTN;
            float promoteTop = modeTop - STACK_GAP - PROMOTE_H;
            float hudLeft = Math.max(MARGIN, w - MARGIN - 280f);
            float hudTop = MARGIN;
            return new Layout(w, h, trayLeft, trayTop, modeTop, promoteTop, hudLeft, hudTop);
        }

        public int trayForwardCenterX() {
            return (int) (trayLeft + TRAY_BTN / 2f);
        }

        public int trayBackCenterX() {
            return (int) (trayLeft + TRAY_BTN + TRAY_GAP + TRAY_BTN / 2f);
        }

        public int trayCenterY() {
            return (int) (trayTop + TRAY_BTN / 2f);
        }

        public int modeGhostCenterX() {
            return (int) (trayLeft + MODE_BTN_W / 2f);
        }

        public int modeDraftCenterX() {
            return (int) (trayLeft + MODE_BTN_W + TRAY_GAP + MODE_BTN_W / 2f);
        }

        public int modeCenterY() {
            return (int) (modeTop + TRAY_BTN / 2f);
        }

        public int promoteCenterX() {
            return (int) (trayLeft + PROMOTE_W / 2f);
        }

        public int promoteCenterY() {
            return (int) (promoteTop + PROMOTE_H / 2f);
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

    public static int modeGhostCenterX() {
        return Layout.forSize(DEFAULT_W, DEFAULT_H).modeGhostCenterX();
    }

    public static int modeDraftCenterX() {
        return Layout.forSize(DEFAULT_W, DEFAULT_H).modeDraftCenterX();
    }

    public static int modeCenterY() {
        return Layout.forSize(DEFAULT_W, DEFAULT_H).modeCenterY();
    }

    public static int promoteCenterX() {
        return Layout.forSize(DEFAULT_W, DEFAULT_H).promoteCenterX();
    }

    public static int promoteCenterY() {
        return Layout.forSize(DEFAULT_W, DEFAULT_H).promoteCenterY();
    }

    public static ChromeHit hitTest(int inputX, int inputY) {
        return hitTest(inputX, inputY, DEFAULT_W, DEFAULT_H);
    }

    public static ChromeHit hitTest(int inputX, int inputY, int screenWidth, int screenHeight) {
        return hitTest(inputX, inputY, Layout.forSize(screenWidth, screenHeight));
    }

    public static ChromeHit hitTest(int inputX, int inputY, Layout layout) {
        if (inRect(inputX, inputY, layout.trayLeft, layout.trayTop, TRAY_BTN, TRAY_BTN)) {
            return ChromeHit.TRAY_FORWARD;
        }
        if (inRect(
                inputX,
                inputY,
                layout.trayLeft + TRAY_BTN + TRAY_GAP,
                layout.trayTop,
                TRAY_BTN,
                TRAY_BTN)) {
            return ChromeHit.TRAY_BACK;
        }
        if (inRect(inputX, inputY, layout.trayLeft, layout.modeTop, MODE_BTN_W, TRAY_BTN)) {
            return ChromeHit.MODE_GHOST;
        }
        if (inRect(
                inputX,
                inputY,
                layout.trayLeft + MODE_BTN_W + TRAY_GAP,
                layout.modeTop,
                MODE_BTN_W,
                TRAY_BTN)) {
            return ChromeHit.MODE_DRAFT;
        }
        if (inRect(inputX, inputY, layout.trayLeft, layout.promoteTop, PROMOTE_W, PROMOTE_H)) {
            return ChromeHit.PROMOTE;
        }
        return ChromeHit.NONE;
    }

    public static void render(
            Draw2D draw2d,
            Font font,
            int screenWidth,
            int screenHeight,
            EditMode mode,
            PieceType selected,
            PuzzleMode puzzle,
            List<ConsistencyBadge> badges,
            VerifyResult lastVerify,
            PromoteResult lastPromote) {
        if (draw2d == null) {
            return;
        }
        Layout layout = Layout.forSize(screenWidth, screenHeight);
        draw2d.setOrtho2D(0f, 0f, layout.screenWidth, layout.screenHeight);
        drawTray(draw2d, layout, selected);
        drawModeChips(draw2d, layout, mode);
        drawPromoteButton(draw2d, layout);
        if (font != null) {
            drawHud(font, draw2d, layout, mode, puzzle, badges, lastVerify, lastPromote);
        }
    }

    private static void drawTray(Draw2D g, Layout layout, PieceType selected) {
        float trayDrawY = layout.screenHeight - layout.trayTop - TRAY_BTN;
        g.begin(DrawMode.LINE);
        g.setColor(
                selected == PieceType.FORWARD ? 0.95f : 0.7f,
                selected == PieceType.FORWARD ? 0.85f : 0.7f,
                selected == PieceType.FORWARD ? 0.35f : 0.72f,
                1f);
        g.rect(layout.trayLeft, trayDrawY, TRAY_BTN, TRAY_BTN);
        drawSlash(g, layout.trayLeft, trayDrawY, TRAY_BTN, true);
        float backLeft = layout.trayLeft + TRAY_BTN + TRAY_GAP;
        g.setColor(
                selected == PieceType.BACK ? 0.95f : 0.7f,
                selected == PieceType.BACK ? 0.85f : 0.7f,
                selected == PieceType.BACK ? 0.35f : 0.72f,
                1f);
        g.rect(backLeft, trayDrawY, TRAY_BTN, TRAY_BTN);
        drawSlash(g, backLeft, trayDrawY, TRAY_BTN, false);
        g.end();
    }

    private static void drawModeChips(Draw2D g, Layout layout, EditMode mode) {
        float y = layout.screenHeight - layout.modeTop - TRAY_BTN;
        g.begin(DrawMode.FILLED);
        g.setColor(
                mode == EditMode.GHOST ? 0.35f : 0.25f,
                mode == EditMode.GHOST ? 0.65f : 0.25f,
                mode == EditMode.GHOST ? 0.85f : 0.3f,
                1f);
        g.rect(layout.trayLeft, y, MODE_BTN_W, TRAY_BTN);
        g.setColor(
                mode == EditMode.DRAFT ? 0.85f : 0.25f,
                mode == EditMode.DRAFT ? 0.55f : 0.25f,
                mode == EditMode.DRAFT ? 0.25f : 0.3f,
                1f);
        g.rect(layout.trayLeft + MODE_BTN_W + TRAY_GAP, y, MODE_BTN_W, TRAY_BTN);
        g.end();
    }

    private static void drawPromoteButton(Draw2D g, Layout layout) {
        float y = layout.screenHeight - layout.promoteTop - PROMOTE_H;
        g.begin(DrawMode.LINE);
        g.setColor(0.8f, 0.8f, 0.85f, 1f);
        g.rect(layout.trayLeft, y, PROMOTE_W, PROMOTE_H);
        g.end();
    }

    private static void drawHud(
            Font font,
            Draw2D draw2d,
            Layout layout,
            EditMode mode,
            PuzzleMode puzzle,
            List<ConsistencyBadge> badges,
            VerifyResult lastVerify,
            PromoteResult lastPromote) {
        float line = layout.screenHeight - layout.hudTop - 16f;
        font.drawString("Mode: " + mode.name(), layout.hudLeft, line);
        line -= 28f;
        font.drawString(
                "Remaining: " + puzzle.remaining() + " / " + puzzle.mirrorBudget(),
                layout.hudLeft,
                line);
        line -= 28f;
        font.drawString("Clues / badges:", layout.hudLeft, line);
        List<ConsistencyBadge> safeBadges =
                badges == null ? Collections.<ConsistencyBadge>emptyList() : badges;
        List<ClueDefinition> clues = puzzle.clues();
        for (int i = 0; i < clues.size(); i++) {
            line -= 22f;
            ClueDefinition c = clues.get(i);
            ConsistencyBadge.Kind kind = badgeKindFor(safeBadges, c.entryPort());
            drawBadgeMarker(draw2d, layout.hudLeft - 18f, line, kind);
            String badgeLabel = kind == null ? "-" : kind.name();
            font.drawString(
                    c.entryPort()
                            + " -> "
                            + c.exitPort()
                            + " mask="
                            + c.colorMask()
                            + " ["
                            + badgeLabel
                            + "]",
                    layout.hudLeft,
                    line);
        }
        line -= 36f;
        font.drawString(verifyChrome(puzzle, lastVerify), layout.hudLeft, line);
        line -= 24f;
        font.drawString(promoteChrome(lastPromote), layout.hudLeft, line);
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

    private static ConsistencyBadge.Kind badgeKindFor(List<ConsistencyBadge> badges, Port entry) {
        for (ConsistencyBadge badge : badges) {
            if (badge.entryPort().equals(entry)) {
                return badge.kind();
            }
        }
        return null;
    }

    private static void drawBadgeMarker(
            Draw2D draw2d, float x, float y, ConsistencyBadge.Kind kind) {
        if (kind == null) {
            return;
        }
        draw2d.begin(DrawMode.FILLED);
        switch (kind) {
            case GREEN:
                draw2d.setColor(0.2f, 0.85f, 0.35f, 1f);
                break;
            case RED:
                draw2d.setColor(0.9f, 0.25f, 0.25f, 1f);
                break;
            case NEUTRAL:
            default:
                draw2d.setColor(0.55f, 0.55f, 0.6f, 1f);
                break;
        }
        draw2d.rect(x, y, 12f, 12f);
        draw2d.end();
    }

    private static String verifyChrome(PuzzleMode puzzle, VerifyResult lastVerify) {
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

    private static String promoteChrome(PromoteResult lastPromote) {
        if (lastPromote == null) {
            return "Promote: ready (P / button)";
        }
        switch (lastPromote.kind()) {
            case OK:
                return "Promote: OK";
            case OVER_BUDGET:
                return "Promote: refuse OVER_BUDGET need="
                        + lastPromote.need()
                        + " rem="
                        + lastPromote.remaining();
            case CELL_CONFLICT:
                return "Promote: refuse CELL_CONFLICT";
            default:
                return "Promote: ready (P / button)";
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
