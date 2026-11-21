package io.github.worldsmiths.prismatrix.pvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MatchKernelGuessTest {

    @Test
    void readyFireCorrectGuessEndsWithWinForGuessingSeat() {
        MatchKernel kernel = playingKernelAFirst(MatchRules.classic());
        Port entry = Port.of(Port.Side.T, 0, 8);

        assertTrue(kernel.fire(SeatId.A, entry).accepted());
        assertEquals(SeatId.B, kernel.activeSeat());

        // B guesses A's secret exactly → win
        MatchActionResult guess = kernel.guess(SeatId.B, classicBoardA());

        assertTrue(guess.accepted());
        assertEquals(MatchPhase.ENDED, kernel.phase());
        assertEquals(EndReason.WIN, kernel.endReason());
        assertEquals(SeatId.B, kernel.winner());
        assertNull(kernel.activeSeat());
    }

    @Test
    void orbsOnlyWinsDespiteMirrorMismatch() {
        MatchKernel kernel = playingKernelAFirst(MatchRules.classic());
        assertFalse(kernel.rules().guessMirrors());

        // Same orbs as B secret; mirrors flipped — Orbs Only still wins
        MatchActionResult guess = kernel.guess(SeatId.A, classicBoardBWithDifferentMirrors());

        assertTrue(guess.accepted());
        assertEquals(MatchPhase.ENDED, kernel.phase());
        assertEquals(EndReason.WIN, kernel.endReason());
        assertEquals(SeatId.A, kernel.winner());
    }

    @Test
    void guessMirrorsMissesWhenMirrorsDiffer() {
        MatchRules rules = MatchRules.builder().guessMirrors(true).build();
        MatchKernel kernel = playingKernelAFirst(rules);

        MatchActionResult guess = kernel.guess(SeatId.A, classicBoardBWithDifferentMirrors());

        assertTrue(guess.accepted());
        assertEquals(MatchPhase.PLAYING, kernel.phase());
        assertNull(kernel.endReason());
        assertEquals(SeatId.B, kernel.activeSeat()); // turn ended on miss
    }

    @Test
    void guessMirrorsWinsWhenExactMatch() {
        MatchRules rules = MatchRules.builder().guessMirrors(true).build();
        MatchKernel kernel = playingKernelAFirst(rules);

        MatchActionResult guess = kernel.guess(SeatId.A, classicBoardB());

        assertTrue(guess.accepted());
        assertEquals(MatchPhase.ENDED, kernel.phase());
        assertEquals(EndReason.WIN, kernel.endReason());
        assertEquals(SeatId.A, kernel.winner());
    }

    @Test
    void incorrectGuessBurnsBudgetEndsTurnWithoutBonusFire() {
        MatchRules rules = MatchRules.builder().maxGuesses(2).build();
        MatchKernel kernel = playingKernelAFirst(rules);
        assertEquals(Integer.valueOf(2), kernel.seat(SeatId.A).guessesLeft());

        // Wrong orbs → miss
        MatchActionResult miss = kernel.guess(SeatId.A, classicBoardA());

        assertTrue(miss.accepted());
        assertEquals(MatchPhase.PLAYING, kernel.phase());
        assertEquals(SeatId.B, kernel.activeSeat());
        assertEquals(Integer.valueOf(1), kernel.seat(SeatId.A).guessesLeft());
        assertTrue(kernel.seat(SeatId.A).fireLog().isEmpty()); // no bonus Fire
        assertNull(kernel.lastFirerFire());
    }

    @Test
    void guessAtBudgetZeroIllegalButFireStillOk() {
        MatchRules rules = MatchRules.builder().maxGuesses(1).build();
        MatchKernel kernel = playingKernelAFirst(rules);

        assertTrue(kernel.guess(SeatId.A, classicBoardA()).accepted()); // miss → budget 0
        assertEquals(Integer.valueOf(0), kernel.seat(SeatId.A).guessesLeft());
        assertEquals(SeatId.B, kernel.activeSeat());

        // B fires so A gets another turn
        Port entry = Port.of(Port.Side.T, 0, 8);
        assertTrue(kernel.fire(SeatId.B, entry).accepted());
        assertEquals(SeatId.A, kernel.activeSeat());

        MatchActionResult exhausted = kernel.guess(SeatId.A, classicBoardB());
        assertFalse(exhausted.accepted());
        assertEquals("GUESS_BUDGET_EXHAUSTED", exhausted.reason());
        assertEquals(SeatId.A, kernel.activeSeat());
        assertEquals(MatchPhase.PLAYING, kernel.phase());

        assertTrue(kernel.fire(SeatId.A, entry).accepted());
        assertEquals(SeatId.B, kernel.activeSeat());
        assertEquals(1, kernel.seat(SeatId.A).fireLog().size());
    }

    @Test
    void unlimitedMissesNeverExhaust() {
        MatchKernel kernel = playingKernelAFirst(MatchRules.classic());
        assertNull(kernel.seat(SeatId.A).guessesLeft());

        for (int i = 0; i < 5; i++) {
            assertEquals(SeatId.A, kernel.activeSeat());
            assertTrue(kernel.guess(SeatId.A, classicBoardA()).accepted()); // miss
            assertNull(kernel.seat(SeatId.A).guessesLeft());
            assertEquals(SeatId.B, kernel.activeSeat());
            assertTrue(kernel.fire(SeatId.B, Port.of(Port.Side.T, 0, 8)).accepted());
        }

        assertEquals(MatchPhase.PLAYING, kernel.phase());
        assertNull(kernel.seat(SeatId.A).guessesLeft());
        // Guess still legal after many misses
        assertTrue(kernel.guess(SeatId.A, classicBoardA()).accepted());
    }

    @Test
    void refusesIncompleteHypothesis() {
        MatchKernel kernel = playingKernelAFirst(MatchRules.classic());
        BoardState incomplete =
                BoardState.builder(8)
                        .place(0, 1, PieceType.ORB_R)
                        .place(1, 1, PieceType.ORB_G)
                        .place(2, 1, PieceType.ORB_B)
                        .build();

        MatchActionResult result = kernel.guess(SeatId.A, incomplete);

        assertFalse(result.accepted());
        assertEquals("INCOMPLETE_INVENTORY", result.reason());
        assertEquals(SeatId.A, kernel.activeSeat());
        assertEquals(MatchPhase.PLAYING, kernel.phase());
        assertNull(kernel.seat(SeatId.A).guessesLeft()); // unlimited; no burn on refuse
    }

    private static MatchKernel playingKernelAFirst(MatchRules rules) {
        Random coin =
                new Random() {
                    @Override
                    public boolean nextBoolean() {
                        return true; // Seat A first
                    }
                };
        MatchKernel kernel = new MatchKernel(rules, coin);
        assertTrue(kernel.ready(SeatId.A, classicBoardA()).accepted());
        assertTrue(kernel.ready(SeatId.B, classicBoardB()).accepted());
        assertEquals(SeatId.A, kernel.activeSeat());
        return kernel;
    }

    /** Seat A secret — distinct from B so correct Guess must match the opponent. */
    private static BoardState classicBoardA() {
        return BoardState.builder(8)
                .place(0, 0, PieceType.ORB_R)
                .place(1, 0, PieceType.ORB_G)
                .place(2, 0, PieceType.ORB_B)
                .place(3, 0, PieceType.FORWARD)
                .place(4, 0, PieceType.FORWARD)
                .place(5, 0, PieceType.BACK)
                .place(6, 0, PieceType.BACK)
                .build();
    }

    /** Seat B secret — different orb layout. */
    private static BoardState classicBoardB() {
        return BoardState.builder(8)
                .place(0, 1, PieceType.ORB_R)
                .place(1, 1, PieceType.ORB_G)
                .place(2, 1, PieceType.ORB_B)
                .place(3, 1, PieceType.FORWARD)
                .place(4, 1, PieceType.FORWARD)
                .place(5, 1, PieceType.BACK)
                .place(6, 1, PieceType.BACK)
                .build();
    }

    /** Same orbs as B secret; mirrors flipped — Orbs Only should still win. */
    private static BoardState classicBoardBWithDifferentMirrors() {
        return BoardState.builder(8)
                .place(0, 1, PieceType.ORB_R)
                .place(1, 1, PieceType.ORB_G)
                .place(2, 1, PieceType.ORB_B)
                .place(3, 1, PieceType.BACK)
                .place(4, 1, PieceType.BACK)
                .place(5, 1, PieceType.FORWARD)
                .place(6, 1, PieceType.FORWARD)
                .build();
    }
}
