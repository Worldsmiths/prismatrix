package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.Draw2D;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.core.Input;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.thin3d.Thin3D;
import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import io.github.worldsmiths.prismatrix.sandbox.ConsistencyBadge;
import io.github.worldsmiths.prismatrix.sandbox.ConsistencyEngine;
import io.github.worldsmiths.prismatrix.sandbox.GhostBoard;
import io.github.worldsmiths.prismatrix.sandbox.ObservedShot;
import io.github.worldsmiths.prismatrix.sandbox.PromoteResult;
import io.github.worldsmiths.prismatrix.sandbox.PromoteService;
import java.util.Collections;
import java.util.List;

/**
 * Pitched presentation facade: {@link BoardView} (Thin3D) + {@link ChromeOverlay} (Draw2D/Font).
 * Ghost|Draft edit modes, promote, Verify, and mode-scoped Test Fire (Ghost or Draft board).
 */
public final class Presentation {

    private final PuzzleMode puzzle;
    private final GhostBoard ghosts;
    private final List<ObservedShot> observations;
    private final BoardView boardView;

    private ChromeOverlay.EditMode mode = ChromeOverlay.EditMode.GHOST;
    private PieceType selected;
    private VerifyResult lastVerify;
    private PromoteResult lastPromote;
    private List<ConsistencyBadge> badges = Collections.emptyList();
    /** Last Test Fire entry port; re-traced after board edits / mode changes until cleared. */
    private Port activeTestFirePort;
    private boolean verifyKeyWasDown;
    private boolean promoteKeyWasDown;
    private boolean ghostModeKeyWasDown;
    private boolean draftModeKeyWasDown;

    public Presentation(PuzzleMode puzzle, Thin3D thin3d) {
        if (puzzle == null) {
            throw new IllegalArgumentException("puzzle must not be null");
        }
        if (thin3d == null) {
            throw new IllegalArgumentException("thin3d must not be null");
        }
        this.puzzle = puzzle;
        this.ghosts = new GhostBoard(puzzle.playerBoard());
        this.observations = ObservedShot.fromClues(puzzle.goals());
        this.boardView = new BoardView(thin3d, puzzle.gridSize());
        refreshBadges();
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
        boardView.render(puzzle, ghosts, badges, activeTestFirePort);
        ChromeOverlay.render(
                draw2d,
                font,
                screenWidth,
                screenHeight,
                mode,
                selected,
                puzzle,
                badges,
                lastVerify,
                lastPromote);
    }

    public void dispose() {
        boardView.dispose();
    }

    public GhostBoard ghostBoard() {
        return ghosts;
    }

    public ChromeOverlay.EditMode editMode() {
        return mode;
    }

    /** Last Verify outcome from keyboard, or {@code null} if Verify has not run. */
    public VerifyResult lastVerifyResult() {
        return lastVerify;
    }

    public PromoteResult lastPromoteResult() {
        return lastPromote;
    }

    public List<ConsistencyBadge> badges() {
        return badges;
    }

    private void handleKeys(Input input) {
        boolean verifyDown = input.isKeyDown(Key.ENTER) || input.isKeyDown(Key.V);
        if (verifyDown && !verifyKeyWasDown && puzzle.canVerify()) {
            lastVerify = puzzle.verify();
        }
        verifyKeyWasDown = verifyDown;

        boolean promoteDown = input.isKeyDown(Key.P);
        if (promoteDown && !promoteKeyWasDown) {
            doPromote();
        }
        promoteKeyWasDown = promoteDown;

        boolean ghostDown = input.isKeyDown(Key.G);
        if (ghostDown && !ghostModeKeyWasDown) {
            setMode(ChromeOverlay.EditMode.GHOST);
        }
        ghostModeKeyWasDown = ghostDown;

        boolean draftDown = input.isKeyDown(Key.D);
        if (draftDown && !draftModeKeyWasDown) {
            setMode(ChromeOverlay.EditMode.DRAFT);
        }
        draftModeKeyWasDown = draftDown;
    }

    private void onTap(int inputX, int inputY, int screenWidth, int screenHeight) {
        ChromeOverlay.ChromeHit chrome =
                ChromeOverlay.hitTest(inputX, inputY, screenWidth, screenHeight);
        switch (chrome) {
            case TRAY_FORWARD:
                selected = PieceType.FORWARD;
                return;
            case TRAY_BACK:
                selected = PieceType.BACK;
                return;
            case MODE_GHOST:
                setMode(ChromeOverlay.EditMode.GHOST);
                return;
            case MODE_DRAFT:
                setMode(ChromeOverlay.EditMode.DRAFT);
                return;
            case PROMOTE:
                doPromote();
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

    private void setMode(ChromeOverlay.EditMode next) {
        if (mode == next) {
            return;
        }
        mode = next;
        refreshActiveTestFire();
    }

    private void placeOrRemove(int x, int y) {
        if (mode == ChromeOverlay.EditMode.GHOST) {
            if (ghosts.pieceAt(x, y) != null) {
                ghosts.remove(x, y);
                lastVerify = null;
                refreshBadges();
                refreshActiveTestFire();
                return;
            }
            if (selected != null) {
                PlaceResult result = ghosts.place(x, y, selected);
                if (result == PlaceResult.OK) {
                    lastVerify = null;
                    refreshBadges();
                    refreshActiveTestFire();
                }
            }
            return;
        }
        PieceType existing = puzzle.pieceAt(x, y);
        if (existing != null) {
            puzzle.remove(x, y);
            lastVerify = null;
            refreshActiveTestFire();
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

    /**
     * Re-traces the sticky Test Fire port on the board for the current edit mode (Ghost or Draft).
     */
    private void refreshActiveTestFire() {
        if (activeTestFirePort == null) {
            boardView.clearBeam();
            return;
        }
        TraceResult traced = ConsistencyEngine.testFire(traceBoard(), activeTestFirePort);
        boardView.setBeamPath(activeTestFirePort, traced.visitedCells(), traced.exitPort());
    }

    private BoardState traceBoard() {
        if (mode == ChromeOverlay.EditMode.DRAFT) {
            return puzzle.mergedBoard();
        }
        return ghosts.toBoardState();
    }

    private void doPromote() {
        lastPromote = PromoteService.promote(ghosts, puzzle.playerBoard());
        if (lastPromote.kind() == PromoteResult.Kind.OK) {
            lastVerify = null;
            refreshActiveTestFire();
        }
    }

    private void refreshBadges() {
        badges = ConsistencyEngine.evaluate(ghosts, observations);
    }
}
