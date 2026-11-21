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
 * Promote / Verify refuse chrome.
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

    static final float TRAY_TOP = 400f;
    static final float TRAY_BTN = 48f;
    static final float TRAY_GAP = 16f;
    static final float TRAY_LEFT = 40f;
    static final float HUD_LEFT = 400f;
    static final float MODE_TOP = 460f;
    static final float MODE_BTN_W = 72f;
    static final float PROMOTE_TOP = 520f;
    static final float PROMOTE_W = 100f;
    static final float PROMOTE_H = 36f;

    private ChromeOverlay() {}

    public static int trayForwardCenterX() {
        return (int) (TRAY_LEFT + TRAY_BTN / 2f);
    }

    public static int trayBackCenterX() {
        return (int) (TRAY_LEFT + TRAY_BTN + TRAY_GAP + TRAY_BTN / 2f);
    }

    public static int trayCenterY() {
        return (int) (TRAY_TOP + TRAY_BTN / 2f);
    }

    public static int modeGhostCenterX() {
        return (int) (TRAY_LEFT + MODE_BTN_W / 2f);
    }

    public static int modeDraftCenterX() {
        return (int) (TRAY_LEFT + MODE_BTN_W + TRAY_GAP + MODE_BTN_W / 2f);
    }

    public static int modeCenterY() {
        return (int) (MODE_TOP + TRAY_BTN / 2f);
    }

    public static int promoteCenterX() {
        return (int) (TRAY_LEFT + PROMOTE_W / 2f);
    }

    public static int promoteCenterY() {
        return (int) (PROMOTE_TOP + PROMOTE_H / 2f);
    }

    public static ChromeHit hitTest(int inputX, int inputY) {
        if (inRect(inputX, inputY, TRAY_LEFT, TRAY_TOP, TRAY_BTN, TRAY_BTN)) {
            return ChromeHit.TRAY_FORWARD;
        }
        if (inRect(
                inputX,
                inputY,
                TRAY_LEFT + TRAY_BTN + TRAY_GAP,
                TRAY_TOP,
                TRAY_BTN,
                TRAY_BTN)) {
            return ChromeHit.TRAY_BACK;
        }
        if (inRect(inputX, inputY, TRAY_LEFT, MODE_TOP, MODE_BTN_W, TRAY_BTN)) {
            return ChromeHit.MODE_GHOST;
        }
        if (inRect(
                inputX,
                inputY,
                TRAY_LEFT + MODE_BTN_W + TRAY_GAP,
                MODE_TOP,
                MODE_BTN_W,
                TRAY_BTN)) {
            return ChromeHit.MODE_DRAFT;
        }
        if (inRect(inputX, inputY, TRAY_LEFT, PROMOTE_TOP, PROMOTE_W, PROMOTE_H)) {
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
        int w = screenWidth > 0 ? screenWidth : 800;
        int h = screenHeight > 0 ? screenHeight : 600;
        draw2d.setOrtho2D(0f, 0f, w, h);
        drawTray(draw2d, h, selected);
        drawModeChips(draw2d, h, mode);
        drawPromoteButton(draw2d, h);
        if (font != null) {
            drawHud(font, draw2d, h, mode, puzzle, badges, lastVerify, lastPromote);
        }
    }

    private static void drawTray(Draw2D g, int screenHeight, PieceType selected) {
        float trayDrawY = screenHeight - TRAY_TOP - TRAY_BTN;
        g.begin(DrawMode.LINE);
        g.setColor(
                selected == PieceType.FORWARD ? 0.95f : 0.7f,
                selected == PieceType.FORWARD ? 0.85f : 0.7f,
                selected == PieceType.FORWARD ? 0.35f : 0.72f,
                1f);
        g.rect(TRAY_LEFT, trayDrawY, TRAY_BTN, TRAY_BTN);
        drawSlash(g, TRAY_LEFT, trayDrawY, TRAY_BTN, true);
        float backLeft = TRAY_LEFT + TRAY_BTN + TRAY_GAP;
        g.setColor(
                selected == PieceType.BACK ? 0.95f : 0.7f,
                selected == PieceType.BACK ? 0.85f : 0.7f,
                selected == PieceType.BACK ? 0.35f : 0.72f,
                1f);
        g.rect(backLeft, trayDrawY, TRAY_BTN, TRAY_BTN);
        drawSlash(g, backLeft, trayDrawY, TRAY_BTN, false);
        g.end();
    }

    private static void drawModeChips(Draw2D g, int screenHeight, EditMode mode) {
        float y = screenHeight - MODE_TOP - TRAY_BTN;
        g.begin(DrawMode.FILLED);
        g.setColor(
                mode == EditMode.GHOST ? 0.35f : 0.25f,
                mode == EditMode.GHOST ? 0.65f : 0.25f,
                mode == EditMode.GHOST ? 0.85f : 0.3f,
                1f);
        g.rect(TRAY_LEFT, y, MODE_BTN_W, TRAY_BTN);
        g.setColor(
                mode == EditMode.DRAFT ? 0.85f : 0.25f,
                mode == EditMode.DRAFT ? 0.55f : 0.25f,
                mode == EditMode.DRAFT ? 0.25f : 0.3f,
                1f);
        g.rect(TRAY_LEFT + MODE_BTN_W + TRAY_GAP, y, MODE_BTN_W, TRAY_BTN);
        g.end();
    }

    private static void drawPromoteButton(Draw2D g, int screenHeight) {
        float y = screenHeight - PROMOTE_TOP - PROMOTE_H;
        g.begin(DrawMode.LINE);
        g.setColor(0.8f, 0.8f, 0.85f, 1f);
        g.rect(TRAY_LEFT, y, PROMOTE_W, PROMOTE_H);
        g.end();
    }

    private static void drawHud(
            Font font,
            Draw2D draw2d,
            int screenHeight,
            EditMode mode,
            PuzzleMode puzzle,
            List<ConsistencyBadge> badges,
            VerifyResult lastVerify,
            PromoteResult lastPromote) {
        float line = screenHeight - 48f;
        font.drawString("Mode: " + mode.name(), HUD_LEFT, line);
        line -= 28f;
        font.drawString(
                "Remaining: " + puzzle.remaining() + " / " + puzzle.mirrorBudget(), HUD_LEFT, line);
        line -= 28f;
        font.drawString("Clues / badges:", HUD_LEFT, line);
        List<ConsistencyBadge> safeBadges =
                badges == null ? Collections.<ConsistencyBadge>emptyList() : badges;
        List<ClueDefinition> clues = puzzle.clues();
        for (int i = 0; i < clues.size(); i++) {
            line -= 22f;
            ClueDefinition c = clues.get(i);
            ConsistencyBadge.Kind kind = badgeKindFor(safeBadges, c.entryPort());
            drawBadgeMarker(draw2d, HUD_LEFT - 18f, line, kind);
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
                    HUD_LEFT,
                    line);
        }
        line -= 36f;
        font.drawString(verifyChrome(puzzle, lastVerify), HUD_LEFT, line);
        line -= 24f;
        font.drawString(promoteChrome(lastPromote), HUD_LEFT, line);
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
