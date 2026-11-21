package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.core.Draw2D;
import io.github.worldsmiths.core.Font;
import io.github.worldsmiths.core.Input;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.thin3d.Thin3D;
import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.puzzle.BoardView;
import io.github.worldsmiths.prismatrix.puzzle.PlaceResult;
import io.github.worldsmiths.prismatrix.puzzle.PlacementBoard;
import java.util.Random;

/**
 * Pitched PvP vs AI facade: one {@link BoardView} phase-switched between deduction (own turn) and
 * own secret + owner beam (IncomingFire). Firer HUD is exit+color only — never feeds opponent
 * {@code visitedCells} into BoardView. AI seat locks human input.
 */
public final class PvpPresentation {

    public enum UiPhase {
        SETUP,
        OWN_TURN,
        INCOMING_FIRE,
        AI_TURN,
        ENDED
    }

    private final MatchKernel kernel;
    private final SeatId humanSeat;
    private final SeatId aiSeat;
    private final AiPolicy ai;
    private final Random random;
    private final BoardView boardView;

    private final PlacementBoard secretBoard;
    private final PlacementBoard deductionBoard;

    private UiPhase uiPhase = UiPhase.SETUP;
    private PieceType selected;
    private FireObservation lastFirerHud;
    private FireObservation pendingOwnerBeam;
    private boolean aiActedThisTurn;
    private boolean readyKeyWasDown;
    private boolean guessKeyWasDown;

    public PvpPresentation(
            MatchKernel kernel,
            SeatId humanSeat,
            SeatId aiSeat,
            AiPolicy ai,
            Thin3D thin3d,
            Random random) {
        if (kernel == null) {
            throw new IllegalArgumentException("kernel must not be null");
        }
        if (humanSeat == null || aiSeat == null) {
            throw new IllegalArgumentException("seats must not be null");
        }
        if (ai == null) {
            throw new IllegalArgumentException("ai must not be null");
        }
        if (thin3d == null) {
            throw new IllegalArgumentException("thin3d must not be null");
        }
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.kernel = kernel;
        this.humanSeat = humanSeat;
        this.aiSeat = aiSeat;
        this.ai = ai;
        this.random = random;
        int n = kernel.rules().gridSize();
        this.secretBoard = PlacementBoard.classic(n);
        this.deductionBoard = PlacementBoard.classic(n);
        this.boardView = new BoardView(thin3d, n);
        syncPhaseFromKernel();
    }

    public UiPhase uiPhase() {
        return uiPhase;
    }

    /** True while AI seat is active or IncomingFire is showing (human cannot edit/fire). */
    public boolean inputLocked() {
        return uiPhase == UiPhase.AI_TURN || uiPhase == UiPhase.INCOMING_FIRE;
    }

    public FireObservation lastFirerHud() {
        return lastFirerHud;
    }

    public PlacementBoard deductionBoard() {
        return deductionBoard;
    }

    public PlacementBoard secretBoard() {
        return secretBoard;
    }

    /** Human Fire verb — HUD only; never pushes opponent path into BoardView. */
    public MatchActionResult fireAsHuman(Port port) {
        if (kernel.phase() != MatchPhase.PLAYING || kernel.activeSeat() != humanSeat) {
            return MatchActionResult.refused("OUT_OF_TURN");
        }
        MatchActionResult result = kernel.fire(humanSeat, port);
        if (result.accepted()) {
            lastFirerHud = kernel.lastFirerFire();
            boardView.clearBeam();
            pendingOwnerBeam = null;
            aiActedThisTurn = false;
            syncPhaseFromKernel();
        }
        return result;
    }

    public void update(Input input, int screenWidth, int screenHeight) {
        boardView.ensureSized(screenWidth, screenHeight);
        syncPhaseFromKernel();
        if (uiPhase == UiPhase.AI_TURN) {
            runAiTurn();
            syncPhaseFromKernel();
        }
        if (input == null || inputLocked()) {
            return;
        }
        if (input.justTouched()) {
            onTap(input.getX(), input.getY(), screenWidth, screenHeight);
        }
        handleKeys(input);
    }

    public void render(Draw2D draw2d, Font font, int screenWidth, int screenHeight) {
        boardView.ensureSized(screenWidth, screenHeight);
        BoardState bound = boundBoard();
        Port highlight = null;
        if (uiPhase == UiPhase.INCOMING_FIRE && pendingOwnerBeam != null) {
            boardView.setBeamPath(
                    pendingOwnerBeam.entryPort(),
                    pendingOwnerBeam.visitedCells(),
                    pendingOwnerBeam.exitPort());
            highlight = pendingOwnerBeam.entryPort();
        } else if (uiPhase != UiPhase.INCOMING_FIRE) {
            boardView.clearBeam();
        }
        boardView.renderBoard(bound, highlight);
        PlacementBoard tray =
                uiPhase == UiPhase.SETUP
                        ? secretBoard
                        : (uiPhase == UiPhase.OWN_TURN ? deductionBoard : null);
        boolean canReady =
                uiPhase == UiPhase.SETUP
                        && secretBoard.isCompleteClassic()
                        && !kernel.seat(humanSeat).ready();
        // Firer HUD only when human was the firer (never show owner path as HUD beam).
        FireObservation hud =
                (lastFirerHud != null && lastFirerHud.visitedCells() == null) ? lastFirerHud : null;
        PvpChrome.render(
                draw2d,
                font,
                screenWidth,
                screenHeight,
                uiPhase,
                selected,
                tray,
                hud,
                inputLocked(),
                canReady);
    }

    public void dispose() {
        boardView.dispose();
    }

    private BoardState boundBoard() {
        if (uiPhase == UiPhase.SETUP) {
            return secretBoard.toBoardState();
        }
        if (uiPhase == UiPhase.INCOMING_FIRE || uiPhase == UiPhase.AI_TURN) {
            BoardState secret = kernel.seat(humanSeat).secret();
            if (secret != null) {
                return secret;
            }
            return secretBoard.toBoardState();
        }
        return deductionBoard.toBoardState();
    }

    private void syncPhaseFromKernel() {
        MatchPhase phase = kernel.phase();
        if (phase == MatchPhase.ENDED) {
            uiPhase = UiPhase.ENDED;
            boardView.clearBeam();
            pendingOwnerBeam = null;
            return;
        }
        if (phase == MatchPhase.SETUP) {
            uiPhase = UiPhase.SETUP;
            return;
        }
        // PLAYING
        SeatId active = kernel.activeSeat();
        if (active == aiSeat) {
            uiPhase = UiPhase.AI_TURN;
            return;
        }
        if (active == humanSeat) {
            if (pendingOwnerBeam != null) {
                uiPhase = UiPhase.INCOMING_FIRE;
            } else {
                uiPhase = UiPhase.OWN_TURN;
            }
        }
    }

    private void runAiTurn() {
        if (aiActedThisTurn || kernel.activeSeat() != aiSeat) {
            return;
        }
        if (!kernel.seat(aiSeat).ready()) {
            BoardState setup = ai.proposeSetup(kernel.rules(), random);
            kernel.ready(aiSeat, setup);
            syncPhaseFromKernel();
            return;
        }
        MatchView view = kernel.viewFor(aiSeat);
        AiAction action = ai.decide(view, random);
        if (action.kind() == AiAction.Kind.FIRE) {
            MatchActionResult fired = kernel.fire(aiSeat, action.port());
            if (fired.accepted()) {
                // Owner beam for human — full path via seat-gated MatchView. Never firer HUD.
                pendingOwnerBeam = kernel.viewFor(humanSeat).lastOwnerFire();
                lastFirerHud = null;
                aiActedThisTurn = true;
            }
        } else {
            MatchActionResult guessed = kernel.guess(aiSeat, action.hypothesis());
            if (guessed.accepted()) {
                pendingOwnerBeam = null;
                boardView.clearBeam();
                aiActedThisTurn = true;
            }
        }
    }

    private void handleKeys(Input input) {
        boolean readyDown = input.isKeyDown(Key.ENTER) || input.isKeyDown(Key.R);
        if (readyDown && !readyKeyWasDown && uiPhase == UiPhase.SETUP) {
            tryReady();
        }
        readyKeyWasDown = readyDown;

        boolean guessDown = input.isKeyDown(Key.G);
        if (guessDown && !guessKeyWasDown && uiPhase == UiPhase.OWN_TURN) {
            tryGuess();
        }
        guessKeyWasDown = guessDown;
    }

    private void onTap(int inputX, int inputY, int screenWidth, int screenHeight) {
        if (uiPhase == UiPhase.INCOMING_FIRE) {
            // Dismiss incoming beam → own turn deduction board.
            pendingOwnerBeam = null;
            boardView.clearBeam();
            uiPhase = UiPhase.OWN_TURN;
            return;
        }
        if (uiPhase == UiPhase.SETUP) {
            PvpChrome.ChromeHit chrome = PvpChrome.hitTest(inputX, inputY, screenWidth, screenHeight);
            switch (chrome) {
                case TRAY_ORB_R:
                    selected = PieceType.ORB_R;
                    return;
                case TRAY_ORB_G:
                    selected = PieceType.ORB_G;
                    return;
                case TRAY_ORB_B:
                    selected = PieceType.ORB_B;
                    return;
                case TRAY_FORWARD:
                    selected = PieceType.FORWARD;
                    return;
                case TRAY_BACK:
                    selected = PieceType.BACK;
                    return;
                case READY:
                    tryReady();
                    return;
                case NONE:
                default:
                    break;
            }
            BoardView.Pick hit = boardView.pick(inputX, inputY);
            if (hit.cell() != null) {
                placeOrRemove(secretBoard, hit.cell());
            }
            return;
        }
        if (uiPhase != UiPhase.OWN_TURN) {
            return;
        }
        PvpChrome.ChromeHit chrome = PvpChrome.hitTest(inputX, inputY, screenWidth, screenHeight);
        switch (chrome) {
            case TRAY_ORB_R:
                selected = PieceType.ORB_R;
                return;
            case TRAY_ORB_G:
                selected = PieceType.ORB_G;
                return;
            case TRAY_ORB_B:
                selected = PieceType.ORB_B;
                return;
            case TRAY_FORWARD:
                selected = PieceType.FORWARD;
                return;
            case TRAY_BACK:
                selected = PieceType.BACK;
                return;
            case READY:
            case NONE:
            default:
                break;
        }
        BoardView.Pick hit = boardView.pick(inputX, inputY);
        if (hit.port() != null) {
            fireAsHuman(hit.port());
            return;
        }
        if (hit.cell() != null) {
            placeOrRemove(deductionBoard, hit.cell());
        }
    }

    private void placeOrRemove(PlacementBoard board, Cell cell) {
        if (board.pieceAt(cell.x(), cell.y()) != null) {
            board.remove(cell.x(), cell.y());
            return;
        }
        if (selected != null) {
            PlaceResult result = board.place(cell.x(), cell.y(), selected);
            if (result != PlaceResult.OK) {
                // ignore illegal
            }
        }
    }

    private void tryReady() {
        if (!secretBoard.isCompleteClassic() || kernel.seat(humanSeat).ready()) {
            return;
        }
        MatchActionResult result = kernel.ready(humanSeat, secretBoard.toBoardState());
        if (result.accepted() && !kernel.seat(aiSeat).ready()) {
            BoardState aiSetup = ai.proposeSetup(kernel.rules(), random);
            kernel.ready(aiSeat, aiSetup);
        }
        aiActedThisTurn = false;
        syncPhaseFromKernel();
    }

    private void tryGuess() {
        if (!deductionBoard.isCompleteClassic()) {
            return;
        }
        MatchActionResult result = kernel.guess(humanSeat, deductionBoard.toBoardState());
        if (result.accepted()) {
            pendingOwnerBeam = null;
            boardView.clearBeam();
            aiActedThisTurn = false;
            syncPhaseFromKernel();
        }
    }
}
