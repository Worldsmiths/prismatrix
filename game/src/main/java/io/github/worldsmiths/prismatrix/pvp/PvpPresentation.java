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
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import io.github.worldsmiths.prismatrix.puzzle.BoardView;
import io.github.worldsmiths.prismatrix.puzzle.PlaceResult;
import io.github.worldsmiths.prismatrix.puzzle.PlacementBoard;
import io.github.worldsmiths.prismatrix.sandbox.ObservedShot;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pitched PvP vs AI facade: one {@link BoardView} phase-switched between deduction (own turn) and
 * own secret + owner beam (IncomingFire). Firer HUD is exit+color only — never feeds opponent
 * {@code visitedCells} into BoardView on real Fire. Own-turn port taps practice-Trace the guess
 * board only. AI seat locks human place/Fire/Guess.
 */
public final class PvpPresentation {

    public enum UiPhase {
        SETUP,
        OWN_TURN,
        /** Local-only linger after human Fire (exit+color HUD); does not block a remote HvH peer. */
        FIRE_RESULT,
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
    private Port selectedPort;
    private TraceResult practiceTrace;
    private FireObservation lastFirerHud;
    private FireObservation pendingOwnerBeam;
    private boolean aiActedThisTurn;
    /** After human Fire: hold FIRE_RESULT until dismiss so HUD is readable before AI acts. */
    private boolean holdFireResult;
    private boolean readyKeyWasDown;
    private boolean guessKeyWasDown;
    private boolean continueKeyWasDown;

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

    /**
     * True while the human cannot place/Fire/Guess. Result overlays still accept dismiss
     * (tap / Enter / Space).
     */
    public boolean inputLocked() {
        return uiPhase == UiPhase.AI_TURN
                || uiPhase == UiPhase.INCOMING_FIRE
                || uiPhase == UiPhase.FIRE_RESULT;
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

    public Port selectedPort() {
        return selectedPort;
    }

    /** Practice Trace on the guess board only (not opponent secret; not a match Fire). */
    public void practicePort(Port port) {
        if (uiPhase != UiPhase.OWN_TURN || port == null) {
            return;
        }
        selectedPort = port;
        practiceTrace = TraceCore.trace(deductionBoard.toBoardState(), port);
    }

    /** Human Fire verb — HUD only; never pushes opponent path into BoardView. */
    public MatchActionResult fireAsHuman(Port port) {
        if (kernel.phase() != MatchPhase.PLAYING || kernel.activeSeat() != humanSeat) {
            return MatchActionResult.refused("OUT_OF_TURN");
        }
        if (holdFireResult || uiPhase == UiPhase.FIRE_RESULT || uiPhase == UiPhase.INCOMING_FIRE) {
            return MatchActionResult.refused("RESULT_PENDING");
        }
        if (uiPhase != UiPhase.OWN_TURN) {
            return MatchActionResult.refused("OUT_OF_TURN");
        }
        MatchActionResult result = kernel.fire(humanSeat, port);
        if (result.accepted()) {
            lastFirerHud = kernel.lastFirerFire();
            boardView.clearBeam();
            practiceTrace = null;
            selectedPort = port;
            pendingOwnerBeam = null;
            aiActedThisTurn = false;
            holdFireResult = true;
            uiPhase = UiPhase.FIRE_RESULT;
        }
        return result;
    }

    public void update(Input input, int screenWidth, int screenHeight) {
        boardView.ensureSized(screenWidth, screenHeight);
        syncPhaseFromKernel();
        if (uiPhase == UiPhase.AI_TURN && !holdFireResult) {
            runAiTurn();
            syncPhaseFromKernel();
        }
        if (input == null) {
            return;
        }
        // Result overlays must accept dismiss even while place/Fire/Guess are locked.
        if (uiPhase == UiPhase.INCOMING_FIRE || uiPhase == UiPhase.FIRE_RESULT) {
            if (input.justTouched()) {
                dismissResultOverlay();
            }
            handleContinueKeys(input);
            return;
        }
        if (uiPhase == UiPhase.AI_TURN) {
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
        } else if (uiPhase == UiPhase.FIRE_RESULT && lastFirerHud != null) {
            // Entry+exit pads only — empty path so opponent visitedCells never reach BoardView.
            boardView.setBeamPath(
                    lastFirerHud.entryPort(),
                    Collections.<Cell>emptyList(),
                    lastFirerHud.exitPort());
            highlight = lastFirerHud.entryPort();
        } else if (uiPhase == UiPhase.OWN_TURN && practiceTrace != null && selectedPort != null) {
            boardView.setBeamPath(
                    selectedPort, practiceTrace.visitedCells(), practiceTrace.exitPort());
            highlight = selectedPort;
        } else {
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
        boolean canFire = uiPhase == UiPhase.OWN_TURN && selectedPort != null;
        boolean canGuess =
                uiPhase == UiPhase.OWN_TURN && deductionBoard.isCompleteClassic();
        FireObservation hud =
                (lastFirerHud != null && lastFirerHud.visitedCells() == null) ? lastFirerHud : null;
        List<ObservedShot> log =
                kernel.phase() == MatchPhase.SETUP
                        ? Collections.<ObservedShot>emptyList()
                        : kernel.viewFor(humanSeat).fireLog();
        PvpChrome.render(
                draw2d,
                font,
                screenWidth,
                screenHeight,
                uiPhase,
                selected,
                tray,
                hud,
                log,
                selectedPort,
                inputLocked(),
                canReady,
                canFire,
                canGuess);
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
            holdFireResult = false;
            return;
        }
        if (phase == MatchPhase.SETUP) {
            uiPhase = UiPhase.SETUP;
            return;
        }
        // Local Fire-result linger wins over kernel active-seat (HvH: peer is not blocked).
        if (holdFireResult) {
            uiPhase = UiPhase.FIRE_RESULT;
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

    private void dismissResultOverlay() {
        if (uiPhase == UiPhase.FIRE_RESULT) {
            holdFireResult = false;
            // Keep lastFirerHud until AI Fire / next human Fire overwrites it.
            syncPhaseFromKernel();
            if (uiPhase == UiPhase.AI_TURN) {
                runAiTurn();
                syncPhaseFromKernel();
            }
            return;
        }
        if (uiPhase == UiPhase.INCOMING_FIRE) {
            pendingOwnerBeam = null;
            boardView.clearBeam();
            uiPhase = UiPhase.OWN_TURN;
        }
    }

    private void runAiTurn() {
        if (aiActedThisTurn || kernel.activeSeat() != aiSeat || holdFireResult) {
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
                // Owner beam for human — full path via seat-gated MatchView. Never firer path HUD.
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

    private void handleContinueKeys(Input input) {
        boolean cont =
                input.isKeyDown(Key.ENTER)
                        || input.isKeyDown(Key.SPACE)
                        || input.isKeyDown(Key.R);
        if (cont && !continueKeyWasDown) {
            dismissResultOverlay();
        }
        continueKeyWasDown = cont;
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
            case FIRE:
                if (selectedPort != null) {
                    fireAsHuman(selectedPort);
                }
                return;
            case GUESS:
                tryGuess();
                return;
            case READY:
            case NONE:
            default:
                break;
        }
        BoardView.Pick hit = boardView.pick(inputX, inputY);
        if (hit.port() != null) {
            practicePort(hit.port());
            return;
        }
        if (hit.cell() != null) {
            placeOrRemove(deductionBoard, hit.cell());
            // Placement changed — refresh practice beam if a port is selected.
            if (selectedPort != null) {
                practicePort(selectedPort);
            }
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
