package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.core.FakeEngineServices;
import io.github.worldsmiths.core.FakeInput;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.thin3d.FakeThin3D;
import io.github.worldsmiths.core.thin3d.Ray;
import io.github.worldsmiths.core.thin3d.Vec3;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.sandbox.PromoteResult;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PresentationTest {

    private static PuzzleMode openBudget1() {
        LevelDefinition level =
                new LevelDefinition(
                        "t",
                        6,
                        1,
                        Collections.singletonList(
                                new ClueDefinition(
                                        Port.of(Port.Side.T, 2, 6),
                                        Port.of(Port.Side.L, 2, 6),
                                        0)));
        return PuzzleMode.open(level);
    }

    private static Presentation pitched(PuzzleMode puzzle, FakeEngineServices services) {
        return new Presentation(puzzle, services.thin3d());
    }

    @Test
    void ghostModeTrayThenPickPlacesGhostNotDraft() {
        PuzzleMode puzzle = openBudget1();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        tap(input, presentation, ChromeOverlay.trayForwardCenterX(), ChromeOverlay.trayCenterY());

        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(0), BoardPlane.cellCenterZ(0)));
        tap(input, presentation, 100, 100);

        assertEquals(PieceType.FORWARD, presentation.ghostBoard().pieceAt(0, 0));
        assertNull(puzzle.pieceAt(0, 0));
        assertEquals(1, puzzle.remaining());
    }

    @Test
    void draftModeTrayThenPickPlacesOnDraft() {
        PuzzleMode puzzle = openBudget1();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        tap(input, presentation, ChromeOverlay.modeDraftCenterX(), ChromeOverlay.modeCenterY());
        assertEquals(ChromeOverlay.EditMode.DRAFT, presentation.editMode());

        tap(input, presentation, ChromeOverlay.trayBackCenterX(), ChromeOverlay.trayCenterY());
        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(1), BoardPlane.cellCenterZ(1)));
        tap(input, presentation, 120, 120);

        assertEquals(PieceType.BACK, puzzle.pieceAt(1, 1));
        assertNull(presentation.ghostBoard().pieceAt(1, 1));
    }

    @Test
    void enterVerifiesWhenBudgetSpent() {
        PuzzleMode puzzle = openBudget1();
        assertEquals(PlaceResult.OK, puzzle.place(2, 2, PieceType.FORWARD));
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();

        input.setKeyDown(Key.ENTER, true);
        presentation.update(input, 800, 600);

        VerifyResult result = presentation.lastVerifyResult();
        assertFalse(result.isRefused());
        assertTrue(result.isOk());
    }

    @Test
    void enterDoesNothingWhileBudgetRemains() {
        PuzzleMode puzzle = openBudget1();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();

        input.setKeyDown(Key.ENTER, true);
        presentation.update(input, 800, 600);

        assertNull(presentation.lastVerifyResult());
    }

    @Test
    void testFirePortStripRecordsEmissiveBoxAndTrs() {
        PuzzleMode puzzle = openBudget1();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        // Prime Thin3D resources (camera / emissive beam model) before clearing the command log.
        presentation.update(input, 800, 600);
        presentation.render(services.draw2d(), null, 800, 600);
        assertTrue(
                containsKind(thin3d.commands(), FakeThin3D.Command.Kind.CREATE_BOX_EMISSIVE),
                "beam model should be createBoxEmissive");
        thin3d.clear();

        int stripX = BoardView.portStripCenterX(Port.Side.T, 2, 6);
        int stripY = BoardView.portStripCenterY(Port.Side.T, 2, 6);
        tap(input, presentation, stripX, stripY);
        presentation.render(services.draw2d(), null, 800, 600);

        List<FakeThin3D.Command> commands = thin3d.commands();
        assertTrue(containsKind(commands, FakeThin3D.Command.Kind.CREATE_INSTANCE));
        assertTrue(containsKind(commands, FakeThin3D.Command.Kind.INSTANCE_TRS));
        FakeThin3D.Command trs = firstKind(commands, FakeThin3D.Command.Kind.INSTANCE_TRS);
        assertEquals(BoardPlane.cellCenterX(2), trs.x, 1e-4f);
        assertEquals(BoardPlane.cellCenterZ(0), trs.z, 1e-4f);
    }

    @Test
    void promoteKeyCopiesGhostsIntoDraft() {
        PuzzleMode puzzle = openBudget1();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        tap(input, presentation, ChromeOverlay.trayForwardCenterX(), ChromeOverlay.trayCenterY());
        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(0), BoardPlane.cellCenterZ(0)));
        tap(input, presentation, 100, 100);

        input.setKeyDown(Key.P, true);
        presentation.update(input, 800, 600);

        assertEquals(PromoteResult.Kind.OK, presentation.lastPromoteResult().kind());
        assertEquals(PieceType.FORWARD, puzzle.pieceAt(0, 0));
        assertEquals(PieceType.FORWARD, presentation.ghostBoard().pieceAt(0, 0));
    }

    private static Ray downwardOnto(float hitX, float hitZ) {
        return new Ray(new Vec3(hitX, 10f, hitZ), new Vec3(0f, -1f, 0f));
    }

    private static void tap(FakeInput input, Presentation presentation, int x, int y) {
        input.setPointer(x, y);
        input.setJustTouched(true);
        presentation.update(input, 800, 600);
        input.setJustTouched(false);
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

    private static FakeThin3D.Command firstKind(
            List<FakeThin3D.Command> commands, FakeThin3D.Command.Kind kind) {
        for (FakeThin3D.Command command : commands) {
            if (command.kind == kind) {
                return command;
            }
        }
        throw new AssertionError("missing command kind " + kind);
    }
}
