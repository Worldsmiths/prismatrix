package io.github.worldsmiths.prismatrix.pvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import io.github.worldsmiths.prismatrix.sandbox.ObservedShot;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MatchKernelFireTest {

    @Test
    void fireSanitizesFirerVisitedCellsButOwnerEventKeepsPath() {
        MatchKernel kernel = playingKernelAFirst();
        Port entry = Port.of(Port.Side.T, 0, 8);

        TraceResult expected = TraceCore.trace(kernel.seat(SeatId.B).secret(), entry);
        assertNotNull(expected.exitPort());
        assertFalse(expected.visitedCells().isEmpty());

        MatchActionResult result = kernel.fire(SeatId.A, entry);

        assertTrue(result.accepted());
        FireObservation firer = kernel.lastFirerFire();
        FireObservation owner = kernel.lastOwnerFire();

        assertNotNull(firer);
        assertNotNull(owner);
        assertNull(firer.visitedCells());
        assertNotNull(owner.visitedCells());
        assertEquals(expected.visitedCells(), owner.visitedCells());
        assertEquals(entry, firer.entryPort());
        assertEquals(expected.exitPort(), firer.exitPort());
        assertEquals(expected.colorMask(), firer.colorMask());
        assertEquals(firer.entryPort(), owner.entryPort());
        assertEquals(firer.exitPort(), owner.exitPort());
        assertEquals(firer.colorMask(), owner.colorMask());

        List<ObservedShot> log = kernel.seat(SeatId.A).fireLog();
        assertEquals(1, log.size());
        assertEquals(entry, log.get(0).entryPort());
        assertEquals(expected.exitPort(), log.get(0).exitPort());
        assertEquals(expected.colorMask(), log.get(0).colorMask());
        assertEquals(SeatId.B, kernel.activeSeat());

        MatchView firerView = kernel.viewFor(SeatId.A);
        assertNull(firerView.lastFirerFire().visitedCells());
        assertNull(firerView.lastOwnerFire());
        assertEquals(1, firerView.fireLog().size());
        MatchView ownerView = kernel.viewFor(SeatId.B);
        assertNotNull(ownerView.lastOwnerFire());
        assertNotNull(ownerView.lastOwnerFire().visitedCells());
        assertTrue(ownerView.fireLog().isEmpty());
    }

    @Test
    void firerMatchViewDoesNotIncludeOwnerPath() {
        MatchKernel kernel = playingKernelAFirst();
        Port entry = Port.of(Port.Side.T, 0, 8);

        assertTrue(kernel.fire(SeatId.A, entry).accepted());

        assertNull(kernel.viewFor(SeatId.A).lastOwnerFire());
        assertNotNull(kernel.viewFor(SeatId.B).lastOwnerFire().visitedCells());
        assertNull(kernel.viewFor(SeatId.A).lastFirerFire().visitedCells());
    }

    @Test
    void afterAiFireThenHumanGuessMissAiMatchViewHasNoOwnerPath() {
        // B = AI firer first; A = human owner then Guess miss.
        Random coin =
                new Random() {
                    @Override
                    public boolean nextBoolean() {
                        return false; // Seat B (AI) first
                    }
                };
        MatchKernel kernel = new MatchKernel(MatchRules.classic(), coin);
        assertTrue(kernel.ready(SeatId.A, classicBoard()).accepted());
        assertTrue(kernel.ready(SeatId.B, classicBoard()).accepted());
        assertEquals(SeatId.B, kernel.activeSeat());

        Port entry = Port.of(Port.Side.T, 0, 8);
        assertTrue(kernel.fire(SeatId.B, entry).accepted());
        assertNotNull(kernel.viewFor(SeatId.A).lastOwnerFire().visitedCells());
        assertNull(kernel.viewFor(SeatId.B).lastOwnerFire());

        // Wrong orbs → miss; turn returns to AI.
        BoardState wrongGuess =
                BoardState.builder(8)
                        .place(0, 1, PieceType.ORB_R)
                        .place(1, 1, PieceType.ORB_G)
                        .place(2, 1, PieceType.ORB_B)
                        .place(3, 0, PieceType.FORWARD)
                        .place(4, 0, PieceType.FORWARD)
                        .place(5, 0, PieceType.BACK)
                        .place(6, 0, PieceType.BACK)
                        .build();
        assertTrue(kernel.guess(SeatId.A, wrongGuess).accepted());
        assertEquals(SeatId.B, kernel.activeSeat());

        assertNull(kernel.viewFor(SeatId.B).lastOwnerFire());
        assertNull(kernel.viewFor(SeatId.A).lastOwnerFire());
    }

    @Test
    void nullExitFireStoresNullableExitInLog() {
        MatchKernel kernel = playingKernelAFirst();
        // Classic Ready rejects absorbers; swap opponent secret after Ready for absorb Trace.
        kernel.seat(SeatId.B).replaceSecret(absorberBoard());
        Port entry = Port.of(Port.Side.T, 3, 8);

        TraceResult traced = TraceCore.trace(kernel.seat(SeatId.B).secret(), entry);
        assertNull(traced.exitPort());

        assertTrue(kernel.fire(SeatId.A, entry).accepted());

        FireObservation firer = kernel.lastFirerFire();
        assertNull(firer.exitPort());
        assertNull(firer.visitedCells());
        assertEquals(0, firer.colorMask());

        FireObservation owner = kernel.lastOwnerFire();
        assertNull(owner.exitPort());
        assertNotNull(owner.visitedCells());

        ObservedShot logged = kernel.seat(SeatId.A).fireLog().get(0);
        assertNull(logged.exitPort());
        assertEquals(entry, logged.entryPort());
    }

    @Test
    void refusesIllegalPortWithoutAppendingLog() {
        MatchKernel kernel = playingKernelAFirst();
        Port oob = Port.of(Port.Side.T, 8, 9); // index 8 is OOB for Classic 8×8

        MatchActionResult result = kernel.fire(SeatId.A, oob);

        assertFalse(result.accepted());
        assertEquals("ILLEGAL_PORT", result.reason());
        assertTrue(kernel.seat(SeatId.A).fireLog().isEmpty());
        assertNull(kernel.lastFirerFire());
        assertEquals(SeatId.A, kernel.activeSeat());
    }

    @Test
    void refusesOutOfTurnFireWithoutAppendingLog() {
        MatchKernel kernel = playingKernelAFirst();
        Port entry = Port.of(Port.Side.T, 0, 8);

        MatchActionResult result = kernel.fire(SeatId.B, entry);

        assertFalse(result.accepted());
        assertEquals("OUT_OF_TURN", result.reason());
        assertTrue(kernel.seat(SeatId.B).fireLog().isEmpty());
        assertTrue(kernel.seat(SeatId.A).fireLog().isEmpty());
        assertEquals(SeatId.A, kernel.activeSeat());
    }

    @Test
    void samePortFireAppendsNewLogRow() {
        MatchKernel kernel = playingKernelAFirst();
        Port entry = Port.of(Port.Side.T, 0, 8);

        assertTrue(kernel.fire(SeatId.A, entry).accepted());
        // B's turn — fire back so A can fire again
        assertTrue(kernel.fire(SeatId.B, entry).accepted());
        assertTrue(kernel.fire(SeatId.A, entry).accepted());

        assertEquals(2, kernel.seat(SeatId.A).fireLog().size());
        assertEquals(entry, kernel.seat(SeatId.A).fireLog().get(0).entryPort());
        assertEquals(entry, kernel.seat(SeatId.A).fireLog().get(1).entryPort());
    }

    private static MatchKernel playingKernelAFirst() {
        Random coin =
                new Random() {
                    @Override
                    public boolean nextBoolean() {
                        return true; // Seat A first
                    }
                };
        MatchKernel kernel = new MatchKernel(MatchRules.classic(), coin);
        assertTrue(kernel.ready(SeatId.A, classicBoard()).accepted());
        assertTrue(kernel.ready(SeatId.B, classicBoard()).accepted());
        assertEquals(SeatId.A, kernel.activeSeat());
        return kernel;
    }

    /** Absorber on T3 corridor → null Trace exit (not Classic-legal for Ready). */
    private static BoardState absorberBoard() {
        return BoardState.builder(8).place(3, 1, PieceType.ABSORBER).build();
    }

    private static BoardState classicBoard() {
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
}
