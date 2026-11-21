package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import io.github.worldsmiths.prismatrix.sandbox.ObservedShot;
import java.util.Random;

/**
 * Headless dual-seat Classic match FSM. Fire traces the opponent secret and appends a sanitized
 * firer log; Guess compares hypothesis under Orbs Only / GuessMirrors and can End(win).
 */
public final class MatchKernel {

    private static final int CLASSIC_ORB_COUNT = 3;
    private static final int CLASSIC_MIRROR_COUNT = 4;

    private final MatchRules rules;
    private final Random random;
    private final Seat seatA;
    private final Seat seatB;

    private MatchPhase phase = MatchPhase.SETUP;
    private SeatId activeSeat;
    private EndReason endReason;
    private SeatId winner;
    private SeatId forfeiter;

    private float setupElapsed;
    private float matchElapsed;

    private FireObservation lastFirerFire;
    private FireObservation lastOwnerFire;

    public MatchKernel(MatchRules rules, Random random) {
        if (rules == null) {
            throw new IllegalArgumentException("rules must not be null");
        }
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        this.rules = rules;
        this.random = random;
        this.seatA = new Seat(SeatId.A, rules.maxGuesses());
        this.seatB = new Seat(SeatId.B, rules.maxGuesses());
    }

    public MatchRules rules() {
        return rules;
    }

    public MatchPhase phase() {
        return phase;
    }

    /** Active seat while {@link MatchPhase#PLAYING}; null otherwise. */
    public SeatId activeSeat() {
        return activeSeat;
    }

    public EndReason endReason() {
        return endReason;
    }

    public SeatId winner() {
        return winner;
    }

    public SeatId forfeiter() {
        return forfeiter;
    }

    public Seat seat(SeatId id) {
        return id == SeatId.A ? seatA : seatB;
    }

    /** Last accepted Fire as seen by the firer ({@code visitedCells == null}); null if none. */
    public FireObservation lastFirerFire() {
        return lastFirerFire;
    }

    /** Last accepted Fire as seen by the owner (full path); null if none. */
    public FireObservation lastOwnerFire() {
        return lastOwnerFire;
    }

    /** Sanitized snapshot for {@code viewer} (own log + Fire channels; no opponent secret). */
    public MatchView viewFor(SeatId viewer) {
        Seat seat = seat(viewer);
        return new MatchView(
                viewer,
                phase,
                activeSeat,
                rules.gridSize(),
                seat.fireLog(),
                lastFirerFire,
                lastOwnerFire,
                seat.guessesLeft());
    }

    /**
     * Accept a Classic secret board for {@code seat}. Untimed matches require explicit Ready;
     * incomplete Classic inventory is refused.
     */
    public MatchActionResult ready(SeatId seatId, BoardState board) {
        if (phase != MatchPhase.SETUP) {
            return MatchActionResult.refused("NOT_SETUP");
        }
        Seat seat = seat(seatId);
        if (seat.ready()) {
            return MatchActionResult.refused("ALREADY_READY");
        }
        if (board == null) {
            return MatchActionResult.refused("NULL_BOARD");
        }
        if (board.size() != rules.gridSize()) {
            return MatchActionResult.refused("WRONG_GRID");
        }
        if (!hasClassicInventory(board)) {
            return MatchActionResult.refused("INCOMPLETE_INVENTORY");
        }
        seat.markReady(board);
        if (seatA.ready() && seatB.ready()) {
            beginPlaying();
        }
        return MatchActionResult.ok();
    }

    /**
     * Trace opponent secret from {@code port}, append sanitized {@link ObservedShot} to the
     * firer's log, and publish firer/owner Fire observations. Refuses out-of-turn / illegal port
     * with no log append. Same-port Fire is allowed (new log row). Ends the turn on accept.
     */
    public MatchActionResult fire(SeatId seatId, Port port) {
        MatchActionResult gate = gateTurnVerb(seatId);
        if (!gate.accepted()) {
            return gate;
        }
        if (port == null || !isLegalPort(port)) {
            return MatchActionResult.refused("ILLEGAL_PORT");
        }
        Seat firer = seat(seatId);
        BoardState opponentSecret = seat(seatId.opponent()).secret();
        TraceResult traced = TraceCore.trace(opponentSecret, port);
        ObservedShot shot = new ObservedShot(port, traced.exitPort(), traced.colorMask());
        firer.appendFire(shot);
        lastFirerFire = FireObservation.firer(port, traced.exitPort(), traced.colorMask());
        lastOwnerFire =
                FireObservation.owner(
                        port, traced.exitPort(), traced.colorMask(), traced.visitedCells());
        endTurn();
        return MatchActionResult.ok();
    }

    /**
     * Submit a Classic hypothesis against the opponent secret. Correct Guess → Ended(win).
     * Incorrect → burn budget if capped, end turn, no bonus Fire. Incomplete inventory /
     * budget 0 (when capped) → refuse. Orbs Only ignores mirrors; GuessMirrors requires exact
     * mirrors too.
     */
    public MatchActionResult guess(SeatId seatId, BoardState hypothesis) {
        MatchActionResult gate = gateTurnVerb(seatId);
        if (!gate.accepted()) {
            return gate;
        }
        Seat seat = seat(seatId);
        if (seat.guessBudgetExhausted()) {
            return MatchActionResult.refused("GUESS_BUDGET_EXHAUSTED");
        }
        if (hypothesis == null) {
            return MatchActionResult.refused("NULL_BOARD");
        }
        if (hypothesis.size() != rules.gridSize()) {
            return MatchActionResult.refused("WRONG_GRID");
        }
        if (!hasClassicInventory(hypothesis)) {
            return MatchActionResult.refused("INCOMPLETE_INVENTORY");
        }
        BoardState secret = seat(seatId.opponent()).secret();
        if (matchesHypothesis(hypothesis, secret, rules.guessMirrors())) {
            endWin(seatId);
            return MatchActionResult.ok();
        }
        seat.decrementGuessBudgetOnMiss();
        endTurn();
        return MatchActionResult.ok();
    }

    /**
     * Advance optional setup/match timers. Timed expiry → Ended(forfeit).
     * Untimed rules never auto-forfeit.
     */
    public void update(float deltaSeconds) {
        if (deltaSeconds < 0f || phase == MatchPhase.ENDED) {
            return;
        }
        if (phase == MatchPhase.SETUP && rules.timedSetup()) {
            setupElapsed += deltaSeconds;
            if (setupElapsed >= rules.setupSeconds()) {
                forfeitUnreadyOrBoth();
            }
            return;
        }
        if (phase == MatchPhase.PLAYING && rules.timedMatch()) {
            matchElapsed += deltaSeconds;
            if (matchElapsed >= rules.matchSeconds()) {
                // Active seat times out.
                endForfeit(activeSeat);
            }
        }
    }

    private MatchActionResult gateTurnVerb(SeatId seatId) {
        if (phase != MatchPhase.PLAYING) {
            return MatchActionResult.refused("NOT_PLAYING");
        }
        if (seatId != activeSeat) {
            return MatchActionResult.refused("OUT_OF_TURN");
        }
        return MatchActionResult.ok();
    }

    private boolean isLegalPort(Port port) {
        int n = rules.gridSize();
        try {
            Port.of(port.side(), port.index(), n);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void beginPlaying() {
        phase = MatchPhase.PLAYING;
        activeSeat = random.nextBoolean() ? SeatId.A : SeatId.B;
        matchElapsed = 0f;
    }

    private void endTurn() {
        if (phase != MatchPhase.PLAYING || activeSeat == null) {
            return;
        }
        activeSeat = activeSeat.opponent();
    }

    private void forfeitUnreadyOrBoth() {
        boolean aReady = seatA.ready();
        boolean bReady = seatB.ready();
        if (aReady && !bReady) {
            endForfeit(SeatId.B);
        } else if (bReady && !aReady) {
            endForfeit(SeatId.A);
        } else {
            // Neither ready (or both somehow) — forfeit A as deterministic seat.
            endForfeit(SeatId.A);
        }
    }

    private void endForfeit(SeatId who) {
        phase = MatchPhase.ENDED;
        endReason = EndReason.FORFEIT;
        forfeiter = who;
        winner = who.opponent();
        activeSeat = null;
    }

    private void endWin(SeatId who) {
        phase = MatchPhase.ENDED;
        endReason = EndReason.WIN;
        winner = who;
        forfeiter = null;
        activeSeat = null;
    }

    /**
     * Orbs Only: orb cells (type+position) must match; mirrors ignored. GuessMirrors: every
     * cell must match including mirrors.
     */
    static boolean matchesHypothesis(BoardState hypothesis, BoardState secret, boolean guessMirrors) {
        int size = secret.size();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                PieceType h = hypothesis.pieceAt(x, y);
                PieceType s = secret.pieceAt(x, y);
                if (guessMirrors) {
                    if (h != s) {
                        return false;
                    }
                } else {
                    if (isOrb(h) || isOrb(s)) {
                        if (h != s) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean isOrb(PieceType piece) {
        return piece == PieceType.ORB_R || piece == PieceType.ORB_G || piece == PieceType.ORB_B;
    }

    static boolean hasClassicInventory(BoardState board) {
        int orbs = 0;
        int mirrors = 0;
        int size = board.size();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                PieceType piece = board.pieceAt(x, y);
                if (piece == null) {
                    continue;
                }
                switch (piece) {
                    case ORB_R:
                    case ORB_G:
                    case ORB_B:
                        orbs++;
                        break;
                    case FORWARD:
                    case BACK:
                        mirrors++;
                        break;
                    default:
                        return false;
                }
            }
        }
        return orbs == CLASSIC_ORB_COUNT && mirrors == CLASSIC_MIRROR_COUNT;
    }
}
