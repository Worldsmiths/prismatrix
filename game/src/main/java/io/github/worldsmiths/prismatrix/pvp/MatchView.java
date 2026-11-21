package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.sandbox.ObservedShot;
import java.util.List;

/**
 * Sanitized seat-facing match snapshot. Never exposes opponent secret {@code BoardState} or
 * firer path cells (owner beam is the only full-path Fire channel).
 */
public final class MatchView {

    private final SeatId viewer;
    private final MatchPhase phase;
    private final SeatId activeSeat;
    private final List<ObservedShot> fireLog;
    private final FireObservation lastFirerFire;
    private final FireObservation lastOwnerFire;
    private final Integer guessesLeft;

    MatchView(
            SeatId viewer,
            MatchPhase phase,
            SeatId activeSeat,
            List<ObservedShot> fireLog,
            FireObservation lastFirerFire,
            FireObservation lastOwnerFire,
            Integer guessesLeft) {
        this.viewer = viewer;
        this.phase = phase;
        this.activeSeat = activeSeat;
        this.fireLog = fireLog;
        this.lastFirerFire = lastFirerFire;
        this.lastOwnerFire = lastOwnerFire;
        this.guessesLeft = guessesLeft;
    }

    public SeatId viewer() {
        return viewer;
    }

    public MatchPhase phase() {
        return phase;
    }

    public SeatId activeSeat() {
        return activeSeat;
    }

    /** Viewer's own firer shot log. */
    public List<ObservedShot> fireLog() {
        return fireLog;
    }

    /**
     * Last Fire as firer HUD ({@code visitedCells == null}). Present after any accepted Fire in
     * the match; chrome should only show it when this viewer was the firer.
     */
    public FireObservation lastFirerFire() {
        return lastFirerFire;
    }

    /**
     * Last Fire as owner beam (full path). Present after any accepted Fire; chrome shows it when
     * this viewer owns the target secret.
     */
    public FireObservation lastOwnerFire() {
        return lastOwnerFire;
    }

    /** Null when unlimited. */
    public Integer guessesLeft() {
        return guessesLeft;
    }
}
