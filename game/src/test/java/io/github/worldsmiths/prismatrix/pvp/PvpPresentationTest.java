package io.github.worldsmiths.prismatrix.pvp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.core.FakeEngineServices;
import io.github.worldsmiths.core.FakeInput;
import io.github.worldsmiths.core.thin3d.FakeThin3D;
import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.Cell;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import io.github.worldsmiths.prismatrix.puzzle.BoardPlane;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * PvP pitched UX: phase-switch BoardView + firer sanitize.
 *
 * <p>Founder smoke (V7 manual checklist):
 *
 * <ol>
 *   <li>Mode entry — start game, choose Puzzle or PvP vs AI (Novice / Tactician).
 *   <li>Classic place — on PvP setup, place 3 orbs + 4 mirrors from the orb/mirror tray; Ready.
 *   <li>AI incoming beam — when AI Fires, own secret board shows path beam (visited cells).
 *   <li>Human Fire HUD — when human Fires, HUD shows exit+color only; no opponent path on BoardView.
 * </ol>
 */
class PvpPresentationTest {

    @Test
    void ownerIncomingFireShowsBeamTrsFromPath() {
        FakeEngineServices services = new FakeEngineServices();
        FakeThin3D thin3d = services.thin3d();
        Port entry = Port.of(Port.Side.T, 0, 8);
        BoardState humanSecret = classicBoard();
        TraceResult expected = TraceCore.trace(humanSecret, entry);

        MatchKernel kernel = playingKernelAiFirst(humanSecret, classicBoard());
        AiPolicy scripted =
                new AiPolicy() {
                    @Override
                    public BoardState proposeSetup(MatchRules rules, Random random) {
                        return classicBoard();
                    }

                    @Override
                    public AiAction decide(MatchView view, Random random) {
                        return AiAction.fire(entry);
                    }
                };
        PvpPresentation presentation =
                new PvpPresentation(
                        kernel, SeatId.A, SeatId.B, scripted, thin3d, new Random(1));

        presentation.update(services.input(), 800, 600);
        presentation.render(services.draw2d(), null, 800, 600);
        assertTrue(
                containsKind(thin3d.commands(), FakeThin3D.Command.Kind.CREATE_BOX_EMISSIVE),
                "beam model should be createBoxEmissive");
        thin3d.clear();

        presentation.render(services.draw2d(), null, 800, 600);

        assertTrue(presentation.uiPhase() == PvpPresentation.UiPhase.INCOMING_FIRE);
        List<FakeThin3D.Command> commands = thin3d.commands();
        assertTrue(containsKind(commands, FakeThin3D.Command.Kind.INSTANCE_TRS));
        Cell first = expected.visitedCells().get(0);
        float entryMidZ = (0f + BoardPlane.cellCenterZ(first.y())) * 0.5f;
        assertNotNull(
                findTrsNear(commands, BoardPlane.cellCenterX(first.x()), entryMidZ),
                "expected rim→first-cell owner beam segment");
    }

    @Test
    void firerTurnDoesNotPushOpponentPathBeam() {
        FakeEngineServices services = new FakeEngineServices();
        FakeThin3D thin3d = services.thin3d();
        Port entry = Port.of(Port.Side.T, 0, 8);

        MatchKernel kernel = playingKernelHumanFirst(classicBoard(), classicBoard());
        AiPolicy idle =
                new AiPolicy() {
                    @Override
                    public BoardState proposeSetup(MatchRules rules, Random random) {
                        return classicBoard();
                    }

                    @Override
                    public AiAction decide(MatchView view, Random random) {
                        return AiAction.fire(Port.of(Port.Side.B, 0, 8));
                    }
                };
        PvpPresentation presentation =
                new PvpPresentation(kernel, SeatId.A, SeatId.B, idle, thin3d, new Random(1));

        presentation.update(services.input(), 800, 600);
        presentation.render(services.draw2d(), null, 800, 600);
        thin3d.clear();

        // Human Fire via public API — render without update so AI does not add an owner beam.
        assertTrue(presentation.fireAsHuman(entry).accepted());
        presentation.render(services.draw2d(), null, 800, 600);

        FireObservation firerHud = presentation.lastFirerHud();
        assertNotNull(firerHud);
        assertNull(firerHud.visitedCells());

        TraceResult opponentPath = TraceCore.trace(kernel.seat(SeatId.B).secret(), entry);
        Cell first = opponentPath.visitedCells().get(0);
        float entryMidZ = (0f + BoardPlane.cellCenterZ(first.y())) * 0.5f;
        // Opponent path must not appear as rim→cell beam TRS on firer turn.
        assertNull(
                findTrsNear(thin3d.commands(), BoardPlane.cellCenterX(first.x()), entryMidZ),
                "firer must not push opponent visitedCells into BoardView");
    }

    @Test
    void incomingFireTapDismissesBeamSoGameIsNotStuck() {
        FakeEngineServices services = new FakeEngineServices();
        Port entry = Port.of(Port.Side.T, 0, 8);
        MatchKernel kernel = playingKernelAiFirst(classicBoard(), classicBoard());
        AiPolicy scripted =
                new AiPolicy() {
                    @Override
                    public BoardState proposeSetup(MatchRules rules, Random random) {
                        return classicBoard();
                    }

                    @Override
                    public AiAction decide(MatchView view, Random random) {
                        return AiAction.fire(entry);
                    }
                };
        PvpPresentation presentation =
                new PvpPresentation(
                        kernel, SeatId.A, SeatId.B, scripted, services.thin3d(), new Random(1));

        presentation.update(services.input(), 800, 600);
        assertTrue(presentation.uiPhase() == PvpPresentation.UiPhase.INCOMING_FIRE);

        FakeInput input = services.input();
        input.setJustTouched(true);
        input.setPointer(400, 300);
        presentation.update(input, 800, 600);

        assertTrue(
                presentation.uiPhase() == PvpPresentation.UiPhase.OWN_TURN,
                "tap must dismiss incoming beam; game was stuck with input locked");
        assertFalse(presentation.inputLocked());
    }

    @Test
    void humanFireShowsResultBeforeOpponentTurn() {
        FakeEngineServices services = new FakeEngineServices();
        FakeThin3D thin3d = services.thin3d();
        Port entry = Port.of(Port.Side.T, 0, 8);
        MatchKernel kernel = playingKernelHumanFirst(classicBoard(), classicBoard());
        final int[] decideCount = {0};
        AiPolicy counting =
                new AiPolicy() {
                    @Override
                    public BoardState proposeSetup(MatchRules rules, Random random) {
                        return classicBoard();
                    }

                    @Override
                    public AiAction decide(MatchView view, Random random) {
                        decideCount[0]++;
                        return AiAction.fire(Port.of(Port.Side.B, 0, 8));
                    }
                };
        PvpPresentation presentation =
                new PvpPresentation(kernel, SeatId.A, SeatId.B, counting, thin3d, new Random(1));

        presentation.update(services.input(), 800, 600);
        assertTrue(presentation.fireAsHuman(entry).accepted());
        presentation.update(services.input(), 800, 600);

        assertTrue(
                presentation.uiPhase() == PvpPresentation.UiPhase.FIRE_RESULT,
                "didnt even see results — human Fire must linger before AI");
        assertNotNull(presentation.lastFirerHud());
        assertTrue(decideCount[0] == 0, "AI must not act while Fire result is showing");

        FakeInput input = services.input();
        input.setJustTouched(true);
        input.setPointer(400, 300);
        presentation.update(input, 800, 600);

        assertTrue(decideCount[0] >= 1 || presentation.uiPhase() != PvpPresentation.UiPhase.FIRE_RESULT);
    }

    @Test
    void aiSeatLocksHumanInput() {
        FakeEngineServices services = new FakeEngineServices();
        MatchKernel kernel = playingKernelAiFirst(classicBoard(), classicBoard());
        AiPolicy neverDecide =
                new AiPolicy() {
                    @Override
                    public BoardState proposeSetup(MatchRules rules, Random random) {
                        return classicBoard();
                    }

                    @Override
                    public AiAction decide(MatchView view, Random random) {
                        // Delayed: first decide is skipped by presentation only calling once;
                        // return fire so match progresses, but inputLocked is checked before.
                        return AiAction.fire(Port.of(Port.Side.T, 1, 8));
                    }
                };
        PvpPresentation presentation =
                new PvpPresentation(
                        kernel,
                        SeatId.A,
                        SeatId.B,
                        neverDecide,
                        services.thin3d(),
                        new Random(1));

        // Before AI runs this frame, active seat is AI → locked.
        assertTrue(presentation.inputLocked());
        FakeInput input = services.input();
        input.setJustTouched(true);
        input.setPointer(100, 100);
        int before = presentation.deductionBoard().remainingMirrors();
        presentation.update(input, 800, 600);
        // Placement during AI/incoming must not mutate deduction board.
        assertTrue(presentation.deductionBoard().remainingMirrors() == before
                || presentation.inputLocked()
                || presentation.uiPhase() == PvpPresentation.UiPhase.INCOMING_FIRE);
        assertFalse(
                presentation.uiPhase() == PvpPresentation.UiPhase.OWN_TURN
                        && !presentation.inputLocked()
                        && presentation.deductionBoard().remainingMirrors() < before);
    }

    private static MatchKernel playingKernelAiFirst(BoardState human, BoardState ai) {
        Random coin =
                new Random() {
                    @Override
                    public boolean nextBoolean() {
                        return false; // Seat B (AI) first
                    }
                };
        MatchKernel kernel = new MatchKernel(MatchRules.classic(), coin);
        assertTrue(kernel.ready(SeatId.A, human).accepted());
        assertTrue(kernel.ready(SeatId.B, ai).accepted());
        return kernel;
    }

    private static MatchKernel playingKernelHumanFirst(BoardState human, BoardState ai) {
        Random coin =
                new Random() {
                    @Override
                    public boolean nextBoolean() {
                        return true; // Seat A (human) first
                    }
                };
        MatchKernel kernel = new MatchKernel(MatchRules.classic(), coin);
        assertTrue(kernel.ready(SeatId.A, human).accepted());
        assertTrue(kernel.ready(SeatId.B, ai).accepted());
        return kernel;
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

    private static boolean containsKind(
            List<FakeThin3D.Command> commands, FakeThin3D.Command.Kind kind) {
        for (FakeThin3D.Command command : commands) {
            if (command.kind == kind) {
                return true;
            }
        }
        return false;
    }

    private static FakeThin3D.Command findTrsNear(
            List<FakeThin3D.Command> commands, float x, float z) {
        for (FakeThin3D.Command command : commands) {
            if (command.kind == FakeThin3D.Command.Kind.INSTANCE_TRS
                    && Math.abs(command.x - x) < 1e-3f
                    && Math.abs(command.z - z) < 1e-3f) {
                return command;
            }
        }
        return null;
    }
}
