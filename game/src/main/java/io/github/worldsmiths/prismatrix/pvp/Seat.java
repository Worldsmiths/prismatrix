package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.BoardState;

/** One match seat: secret board after Ready, plus guess budget. */
public final class Seat {

    private final SeatId id;
    private BoardState secret;
    private boolean ready;
    /** Null when unlimited; otherwise remaining guesses. */
    private Integer guessesLeft;

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

    void markReady(BoardState board) {
        this.secret = board;
        this.ready = true;
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
