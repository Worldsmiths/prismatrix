package io.github.worldsmiths.prismatrix.sandbox;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import io.github.worldsmiths.prismatrix.puzzle.GoalDefinition;
import io.github.worldsmiths.prismatrix.puzzle.PlacementBoard;
import io.github.worldsmiths.prismatrix.puzzle.PlaceResult;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConsistencyEngineTest {

    @Test
    void matchingCluePathMissingGhostsIsNeutral() {
        // Empty draft: T1 → B1 color 0. Ghost off the vertical corridor at (0,0).
        PlacementBoard draft = PlacementBoard.mirrorsOnly(3, 2);
        GhostBoard ghosts = new GhostBoard(draft);
        assertEquals(PlaceResult.OK, ghosts.place(0, 0, PieceType.FORWARD));

        Port entry = Port.of(Port.Side.T, 1, 3);
        List<ObservedShot> shots =
                ObservedShot.fromClues(
                        Collections.singletonList(
                                new GoalDefinition(entry, Port.of(Port.Side.B, 1, 3), 0)));

        List<ConsistencyBadge> badges = ConsistencyEngine.evaluate(ghosts, shots);

        assertEquals(1, badges.size());
        assertEquals(entry, badges.get(0).entryPort());
        assertEquals(ConsistencyBadge.Kind.NEUTRAL, badges.get(0).kind());
    }

    @Test
    void pathHittingMatchingGhostIsGreen() {
        PlacementBoard draft = PlacementBoard.mirrorsOnly(3, 1);
        GhostBoard ghosts = new GhostBoard(draft);
        assertEquals(PlaceResult.OK, ghosts.place(1, 1, PieceType.FORWARD));

        Port entry = Port.of(Port.Side.T, 1, 3);
        List<ObservedShot> shots =
                ObservedShot.fromClues(
                        Collections.singletonList(
                                new GoalDefinition(entry, Port.of(Port.Side.L, 1, 3), 0)));

        List<ConsistencyBadge> badges = ConsistencyEngine.evaluate(ghosts, shots);

        assertEquals(ConsistencyBadge.Kind.GREEN, badges.get(0).kind());
    }

    @Test
    void pathHittingMismatchingGhostIsRed() {
        PlacementBoard draft = PlacementBoard.mirrorsOnly(3, 1);
        GhostBoard ghosts = new GhostBoard(draft);
        // `\` at (1,1): T1 → R1, but observation expects L1
        assertEquals(PlaceResult.OK, ghosts.place(1, 1, PieceType.BACK));

        Port entry = Port.of(Port.Side.T, 1, 3);
        List<ObservedShot> shots =
                ObservedShot.fromClues(
                        Collections.singletonList(
                                new GoalDefinition(entry, Port.of(Port.Side.L, 1, 3), 0)));

        List<ConsistencyBadge> badges = ConsistencyEngine.evaluate(ghosts, shots);

        assertEquals(ConsistencyBadge.Kind.RED, badges.get(0).kind());
    }

    @Test
    void testFireTracesGhostBoardOnly() {
        PlacementBoard draft = PlacementBoard.mirrorsOnly(3, 1);
        assertEquals(PlaceResult.OK, draft.place(0, 2, PieceType.FORWARD));
        GhostBoard ghosts = new GhostBoard(draft);
        assertEquals(PlaceResult.OK, ghosts.place(1, 1, PieceType.FORWARD));

        Port entry = Port.of(Port.Side.T, 1, 3);
        TraceResult fired = ConsistencyEngine.testFire(ghosts, entry);

        assertEquals(Port.of(Port.Side.L, 1, 3), fired.exitPort());
        assertTrue(fired.visitedCells().contains(new Cell(1, 1)));
        assertFalse(fired.visitedCells().contains(new Cell(0, 2)));
    }

    @Test
    void nullExitObservationOffPathIsNeutralWithoutNpe() {
        PlacementBoard draft = PlacementBoard.mirrorsOnly(3, 1);
        GhostBoard ghosts = new GhostBoard(draft);
        assertEquals(PlaceResult.OK, ghosts.place(0, 0, PieceType.FORWARD));

        Port entry = Port.of(Port.Side.T, 1, 3);
        List<ObservedShot> shots =
                Collections.singletonList(new ObservedShot(entry, null, 0));

        List<ConsistencyBadge> badges =
                assertDoesNotThrow(() -> ConsistencyEngine.evaluate(ghosts, shots));

        assertEquals(ConsistencyBadge.Kind.NEUTRAL, badges.get(0).kind());
    }

    @Test
    void nullExitObservationPathHitWithRealExitIsRedWithoutNpe() {
        // Ghost `/` at (1,1): T1 → L1, but observation claims absorb (null exit).
        PlacementBoard draft = PlacementBoard.mirrorsOnly(3, 1);
        GhostBoard ghosts = new GhostBoard(draft);
        assertEquals(PlaceResult.OK, ghosts.place(1, 1, PieceType.FORWARD));

        Port entry = Port.of(Port.Side.T, 1, 3);
        List<ObservedShot> shots =
                Collections.singletonList(new ObservedShot(entry, null, 0));

        List<ConsistencyBadge> badges =
                assertDoesNotThrow(() -> ConsistencyEngine.evaluate(ghosts, shots));

        assertEquals(ConsistencyBadge.Kind.RED, badges.get(0).kind());
    }

    @Test
    void matchingNullExitAbsorbObservationsAreGreen() {
        Port entry = Port.of(Port.Side.T, 1, 3);
        ObservedShot shot = new ObservedShot(entry, null, 0);
        TraceResult absorbed =
                new TraceResult(null, 0, 0, Collections.singletonList(new Cell(1, 1)));
        Set<Cell> ghostCells = Collections.singleton(new Cell(1, 1));

        ConsistencyBadge.Kind kind =
                ConsistencyEngine.badgeFor(shot, absorbed, ghostCells);

        assertEquals(ConsistencyBadge.Kind.GREEN, kind);
    }
}
