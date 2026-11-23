package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.Draw2D;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.core.Input;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.thin3d.Thin3D;
import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;

/**
 * Pitched presentation facade: {@link BoardView} (Thin3D) + {@link ChromeOverlay} (Draw2D/Font).
 * Direct placement on the live merged board, sticky Test Fire, and Enter/V Verify.
 */
public final class Presentation {

    private final PuzzleMode puzzle;
    private final BoardView boardView;

    private PieceType selected;
    private VerifyResult lastVerify;
    /** Last Test Fire entry port; re-traced after board edits until cleared. */
    private Port activeTestFirePort;
    private boolean verifyKeyWasDown;

    public Presentation(PuzzleMode puzzle, Thin3D thin3d) {
        if (puzzle == null) {
            throw new IllegalArgumentException("puzzle must not be null");
        }
        if (thin3d == null) {
            throw new IllegalArgumentException("thin3d must not be null");
        }
        this.puzzle = puzzle;
        this.boardView = new BoardView(thin3d, puzzle.gridSize());
        OrbColorPalette.Id paletteId = puzzle.level().orbColors();
        if (paletteId != null) {
            boardView.setColorPalette(OrbColorPalette.of(paletteId));
        }
    }

    public void update(Input input, int screenWidth, int screenHeight) {
        boardView.ensureSized(screenWidth, screenHeight);
        if (input == null) {
            return;
        }
        if (input.justTouched()) {
            onTap(input.getX(), input.getY(), screenWidth, screenHeight);
        }
        handleKeys(input);
    }

    public void render(Draw2D draw2d, Font font, int screenWidth, int screenHeight) {
        boardView.ensureSized(screenWidth, screenHeight);
        boardView.render(puzzle, activeTestFirePort);
        ChromeOverlay.render(
                draw2d,
                font,
                screenWidth,
                screenHeight,
                selected,
                puzzle,
                lastVerify,
                boardView.colorPalette());
    }

    public void dispose() {
        boardView.dispose();
    }

    /** Active orb / beam palette (from level {@code orbColors}, or BoardView default). */
    public OrbColorPalette colorPalette() {
        return boardView.colorPalette();
    }

    /** Last Verify outcome from keyboard, or {@code null} if Verify has not run. */
    public VerifyResult lastVerifyResult() {
        return lastVerify;
    }

    private void handleKeys(Input input) {
        boolean verifyDown = input.isKeyDown(Key.ENTER) || input.isKeyDown(Key.V);
        if (verifyDown && !verifyKeyWasDown && puzzle.canVerify()) {
            lastVerify = puzzle.verify();
        }
        verifyKeyWasDown = verifyDown;
    }

    private void onTap(int inputX, int inputY, int screenWidth, int screenHeight) {
        ChromeOverlay.ChromeHit chrome =
                ChromeOverlay.hitTest(inputX, inputY, screenWidth, screenHeight, puzzle);
        switch (chrome) {
            case TRAY_FORWARD:
                selected = PieceType.FORWARD;
                return;
            case TRAY_BACK:
                selected = PieceType.BACK;
                return;
            case TRAY_ORB_1:
                selected = PieceType.ORB_1;
                return;
            case TRAY_ORB_2:
                selected = PieceType.ORB_2;
                return;
            case TRAY_ORB_3:
                selected = PieceType.ORB_3;
                return;
            case NONE:
            default:
                break;
        }

        BoardView.Pick hit = boardView.pick(inputX, inputY);
        if (hit.port() != null) {
            testFire(hit.port());
            return;
        }
        Cell cell = hit.cell();
        if (cell == null) {
            return;
        }
        placeOrRemove(cell.x(), cell.y());
    }

    private void placeOrRemove(int x, int y) {
        if (puzzle.isFixed(x, y)) {
            return;
        }
        PieceType existing = puzzle.pieceAt(x, y);
        if (existing != null) {
            if (puzzle.remove(x, y)) {
                lastVerify = null;
                refreshActiveTestFire();
            }
            return;
        }
        if (selected != null) {
            PlaceResult result = puzzle.place(x, y, selected);
            if (result == PlaceResult.OK) {
                lastVerify = null;
                refreshActiveTestFire();
            }
        }
    }

    private void testFire(Port port) {
        activeTestFirePort = port;
        refreshActiveTestFire();
    }

    /** Re-traces the sticky Test Fire port on the merged board. */
    private void refreshActiveTestFire() {
        if (activeTestFirePort == null) {
            boardView.clearBeam();
            return;
        }
        TraceResult traced = TraceCore.trace(puzzle.mergedBoard(), activeTestFirePort);
        boardView.setBeamPath(activeTestFirePort, traced.visitedCells(), traced.exitPort());
    }
}
