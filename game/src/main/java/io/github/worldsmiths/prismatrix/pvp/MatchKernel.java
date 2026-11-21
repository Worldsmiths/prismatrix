package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.Random;

/**
 * Headless dual-seat Classic match FSM. Fire Trace / Guess wincon are stubs until later tasks.
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
     * Fire stub (Trace sanitize lands in T3). Refuses out-of-turn / non-playing.
     * On accept, ends the turn (Fire XOR Guess per turn).
     */
    public MatchActionResult fire(SeatId seatId, Port port) {
        MatchActionResult gate = gateTurnVerb(seatId);
        if (!gate.accepted()) {
            return gate;
        }
        // T3 will run Trace against opponent secret and append firer log.
        endTurn();
        return MatchActionResult.ok();
    }

    /**
     * Guess stub (wincon lands in T4). Refuses out-of-turn / budget 0 when capped.
     * Miss path decrements budget via seat hook; correct Guess → Ended(win) deferred to T4.
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
        // T4 will compare hypothesis under Orbs Only / GuessMirrors.
        // Stub treats every Guess as a miss so budget hook is exercised.
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
