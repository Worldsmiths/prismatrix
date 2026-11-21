package io.github.worldsmiths.prismatrix.pvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MatchKernelTest {

    @Test
    void refusesReadyWhenInventoryIncomplete() {
        MatchKernel kernel = new MatchKernel(MatchRules.classic(), new Random(1L));
        BoardState incomplete = BoardState.builder(8)
                .place(0, 0, PieceType.ORB_R)
                .place(1, 0, PieceType.ORB_G)
                .place(2, 0, PieceType.FORWARD)
                .build();

        MatchActionResult result = kernel.ready(SeatId.A, incomplete);

        assertFalse(result.accepted());
        assertEquals(MatchPhase.SETUP, kernel.phase());
    }

    @Test
    void bothReadyCoinFlipActivatesOneSeat() {
        Random coin = new Random() {
            @Override
            public boolean nextBoolean() {
                return true; // Seat A first
            }
        };
        MatchKernel kernel = new MatchKernel(MatchRules.classic(), coin);

        assertTrue(kernel.ready(SeatId.A, classicBoard()).accepted());
        assertEquals(MatchPhase.SETUP, kernel.phase());

        assertTrue(kernel.ready(SeatId.B, classicBoard()).accepted());

        assertEquals(MatchPhase.PLAYING, kernel.phase());
        assertEquals(SeatId.A, kernel.activeSeat());
        assertNotNull(kernel.seat(SeatId.A).secret());
        assertNotNull(kernel.seat(SeatId.B).secret());
    }

    @Test
    void refusesOutOfTurnFireAndGuess() {
        Random coin = new Random() {
            @Override
            public boolean nextBoolean() {
                return true; // Seat A first
            }
        };
        MatchKernel kernel = new MatchKernel(MatchRules.classic(), coin);
        assertTrue(kernel.ready(SeatId.A, classicBoard()).accepted());
        assertTrue(kernel.ready(SeatId.B, classicBoard()).accepted());
        assertEquals(SeatId.A, kernel.activeSeat());

        Port port = Port.of(Port.Side.T, 0, 8);
        MatchActionResult fire = kernel.fire(SeatId.B, port);
        MatchActionResult guess = kernel.guess(SeatId.B, classicBoard());

        assertFalse(fire.accepted());
        assertEquals("OUT_OF_TURN", fire.reason());
        assertFalse(guess.accepted());
        assertEquals("OUT_OF_TURN", guess.reason());
        assertEquals(SeatId.A, kernel.activeSeat());
        assertEquals(MatchPhase.PLAYING, kernel.phase());
    }

    @Test
    void timedSetupExpiryForfeitsWhenDeltaExceedsLimit() {
        MatchRules rules = MatchRules.builder().setupSeconds(5).build();
        MatchKernel kernel = new MatchKernel(rules, new Random(1L));

        kernel.update(5.0f);

        assertEquals(MatchPhase.ENDED, kernel.phase());
        assertEquals(EndReason.FORFEIT, kernel.endReason());
        assertNotNull(kernel.forfeiter());
    }

    @Test
    void untimedNeverAutoForfeitsOnUpdate() {
        MatchKernel kernel = new MatchKernel(MatchRules.classic(), new Random(1L));

        kernel.update(10_000f);
        kernel.update(10_000f);

        assertEquals(MatchPhase.SETUP, kernel.phase());
        assertNull(kernel.endReason());

        assertTrue(kernel.ready(SeatId.A, classicBoard()).accepted());
        assertTrue(kernel.ready(SeatId.B, classicBoard()).accepted());
        SeatId active = kernel.activeSeat();

        kernel.update(10_000f);
        kernel.update(10_000f);

        assertEquals(MatchPhase.PLAYING, kernel.phase());
        assertEquals(active, kernel.activeSeat());
        assertNull(kernel.endReason());
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