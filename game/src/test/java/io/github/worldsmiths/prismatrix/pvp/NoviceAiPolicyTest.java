package io.github.worldsmiths.prismatrix.pvp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import org.junit.jupiter.api.Test;

class NoviceAiPolicyTest {

    @Test
    void seededNoviceFinishesMatchVsScriptedSeat() {
        Random coin =
                new Random() {
                    @Override
                    public boolean nextBoolean() {
                        return true; // Seat A (scripted) first
                    }
                };
        Random aiRng = new Random(42L);
        AiPolicy novice = new NoviceAiPolicy();
        MatchRules rules = MatchRules.classic();
        MatchKernel kernel = new MatchKernel(rules, coin);

        BoardState scriptedBoard = classicBoard();
        BoardState aiBoard = novice.proposeSetup(rules, aiRng);
        assertTrue(MatchKernel.hasClassicInventory(aiBoard));

        assertTrue(kernel.ready(SeatId.A, scriptedBoard).accepted());
        assertTrue(kernel.ready(SeatId.B, aiBoard).accepted());
        assertEquals(MatchPhase.PLAYING, kernel.phase());
        assertEquals(SeatId.A, kernel.activeSeat());

        SeatId scripted = SeatId.A;
        SeatId aiSeat = SeatId.B;
        boolean aiActed = false;
        int safety = 200;
        while (kernel.phase() != MatchPhase.ENDED && safety-- > 0) {
            SeatId active = kernel.activeSeat();
            if (active == aiSeat) {
                MatchView view = kernel.viewFor(aiSeat);
                assertMatchViewHasNoOpponentSecret(view);
                AiAction action = novice.decide(view, aiRng);
                assertNotNull(action);
                apply(kernel, aiSeat, action);
                aiActed = true;
            } else {
                // Scripted seat: Fire once so AI gets a turn, then Guess AI secret (harness peek).
                if (!aiActed) {
                    assertTrue(kernel.fire(scripted, Port.of(Port.Side.T, 0, 8)).accepted());
                } else {
                    assertTrue(kernel.guess(scripted, kernel.seat(aiSeat).secret()).accepted());
                }
            }
        }

        assertEquals(MatchPhase.ENDED, kernel.phase());
        assertEquals(EndReason.WIN, kernel.endReason());
        assertEquals(scripted, kernel.winner());
        assertTrue(aiActed);
        assertTrue(safety > 0);
    }

    @Test
    void matchViewNeverExposesOpponentSecretBoardState() {
        Random coin =
                new Random() {
                    @Override
                    public boolean nextBoolean() {
                        return true;
                    }
                };
        MatchKernel kernel = new MatchKernel(MatchRules.classic(), coin);
        assertTrue(kernel.ready(SeatId.A, classicBoard()).accepted());
        assertTrue(kernel.ready(SeatId.B, classicBoard()).accepted());
        assertTrue(kernel.fire(SeatId.A, Port.of(Port.Side.T, 1, 8)).accepted());

        MatchView forA = kernel.viewFor(SeatId.A);
        MatchView forB = kernel.viewFor(SeatId.B);
        assertMatchViewHasNoOpponentSecret(forA);
        assertMatchViewHasNoOpponentSecret(forB);
    }

    private static void apply(MatchKernel kernel, SeatId seat, AiAction action) {
        MatchActionResult result;
        if (action.kind() == AiAction.Kind.FIRE) {
            result = kernel.fire(seat, action.port());
        } else {
            result = kernel.guess(seat, action.hypothesis());
        }
        assertTrue(result.accepted(), "AI action refused: " + result.reason());
    }

    /**
     * MatchView must not carry any {@link BoardState} (own or opponent). AI may only see
     * sanitized Fire channels + own firer log.
     */
    private static void assertMatchViewHasNoOpponentSecret(MatchView view) {
        assertNotNull(view);
        for (Field field : MatchView.class.getDeclaredFields()) {
            assertFalse(
                    BoardState.class.isAssignableFrom(field.getType()),
                    "MatchView must not hold BoardState field: " + field.getName());
        }
        for (Method method : MatchView.class.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            assertFalse(
                    BoardState.class.isAssignableFrom(method.getReturnType()),
                    "MatchView must not expose BoardState via " + method.getName());
        }
    }

    private static BoardState classicBoard() {
        return BoardState.builder(8)
                .place(0, 0, PieceType.ORB_1)
                .place(1, 0, PieceType.ORB_2)
                .place(2, 0, PieceType.ORB_3)
                .place(3, 0, PieceType.FORWARD)
                .place(4, 0, PieceType.FORWARD)
                .place(5, 0, PieceType.BACK)
                .place(6, 0, PieceType.BACK)
                .build();
    }
}
