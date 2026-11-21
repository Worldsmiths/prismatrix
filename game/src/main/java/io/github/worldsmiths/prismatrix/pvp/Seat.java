package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.sandbox.ObservedShot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One match seat: secret board after Ready, firer shot log, plus guess budget. */
public final class Seat {

    private final SeatId id;
    private BoardState secret;
    private boolean ready;
    /** Null when unlimited; otherwise remaining guesses. */
    private Integer guessesLeft;
    private final List<ObservedShot> fireLog = new ArrayList<ObservedShot>();

    Seat(SeatId id, Integer initialGuesses) {
        this.id = id;
        this.guessesLeft = initialGuesses;
    }

    public SeatId id() {
        return id;
    }

    public boolean ready() {
        return ready;
    }

    /** Secret board after Ready; null before Ready. */
    public BoardState secret() {
        return secret;
    }

    /** Null when unlimited. */
    public Integer guessesLeft() {
        return guessesLeft;
    }

    /** Append-only firer observation log (sanitized exit+color; nullable exit). */
    public List<ObservedShot> fireLog() {
        return Collections.unmodifiableList(fireLog);
    }

    void markReady(BoardState board) {
        this.secret = board;
        this.ready = true;
    }

    /**
     * Test/fixture hook: swap secret after Ready (e.g. absorber null-exit board) without
     * re-checking Classic inventory.
     */
    void replaceSecret(BoardState board) {
        if (board == null) {
            throw new IllegalArgumentException("board must not be null");
        }
        this.secret = board;
    }

    void appendFire(ObservedShot shot) {
        if (shot == null) {
            throw new IllegalArgumentException("shot must not be null");
        }
        fireLog.add(shot);
    }

    /** Decrement on miss when capped; no-op when unlimited. */
    void decrementGuessBudgetOnMiss() {
        if (guessesLeft != null && guessesLeft > 0) {
            guessesLeft = guessesLeft - 1;
        }
    }

    boolean guessBudgetExhausted() {
        return guessesLeft != null && guessesLeft == 0;
    }
}
