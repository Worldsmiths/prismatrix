package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.core.FakeEngineServices;
import io.github.worldsmiths.core.FakeInput;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.core.thin3d.FakeThin3D;
import io.github.worldsmiths.core.thin3d.Ray;
import io.github.worldsmiths.core.thin3d.Vec3;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
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
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 2, 6),
                                        Port.of(Port.Side.L, 2, 6),
                                        0)));
        return PuzzleMode.open(level);
    }

    private static PuzzleMode openWithFixedForwardAt20() {
        LevelDefinition level =
                new LevelDefinition(
                        "fixed",
                        6,
                        1,
                        0,
                        0,
                        0,
                        null,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 2, 6),
                                        Port.of(Port.Side.B, 2, 6),
                                        0)),
                        Collections.singletonList(
                                new FixedPieceDefinition(2, 0, PieceType.FORWARD)));
        return PuzzleMode.open(level);
    }

    private static PuzzleMode openWithOrb() {
        LevelDefinition level =
                new LevelDefinition(
                        "orb",
                        6,
                        0,
                        1,
                        0,
                        0,
                        OrbColorPalette.Id.RED_YELLOW_BLUE,
                        Collections.singletonList(
                                new GoalDefinition(
                                        Port.of(Port.Side.T, 0, 6),
                                        Port.of(Port.Side.B, 0, 6),
                                        PieceType.COLOR_1)),
                        Collections.<FixedPieceDefinition>emptyList());
        return PuzzleMode.open(level);
    }

    private static Presentation pitched(PuzzleMode puzzle, FakeEngineServices services) {
        return new Presentation(puzzle, services.thin3d());
    }

    @Test
    void trayThenPickPlacesOnPlayerBoard() {
        PuzzleMode puzzle = openBudget1();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        tap(input, presentation, ChromeOverlay.trayForwardCenterX(), ChromeOverlay.trayCenterY());

        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(0), BoardPlane.cellCenterZ(0)));
        tap(input, presentation, 100, 100);

        assertEquals(PieceType.FORWARD, puzzle.pieceAt(0, 0));
        assertEquals(0, puzzle.remaining());
    }

    @Test
    void tapOccupiedCellRemovesPiece() {
        PuzzleMode puzzle = openBudget1();
        assertEquals(PlaceResult.OK, puzzle.place(1, 1, PieceType.BACK));
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(1), BoardPlane.cellCenterZ(1)));
        tap(input, presentation, 120, 120);

        assertNull(puzzle.pieceAt(1, 1));
        assertEquals(1, puzzle.remaining());
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

        presentation.update(input, 800, 600);
        presentation.render(services.draw2d(), null, 800, 600);
        assertTrue(
                containsKind(thin3d.commands(), FakeThin3D.Command.Kind.CREATE_BOX_EMISSIVE),
                "beam model should be createBoxEmissive");
        thin3d.clear();

        float margin = BoardPlane.portStripMargin(6);
        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(2), -margin * 0.5f));
        tap(input, presentation, 200, 200);
        presentation.render(services.draw2d(), null, 800, 600);

        List<FakeThin3D.Command> commands = thin3d.commands();
        assertTrue(containsKind(commands, FakeThin3D.Command.Kind.CREATE_INSTANCE));
        assertTrue(containsKind(commands, FakeThin3D.Command.Kind.INSTANCE_TRS));
        float entryMidZ = (0f + BoardPlane.cellCenterZ(0)) * 0.5f;
        FakeThin3D.Command entrySeg =
                findTrsNear(commands, BoardPlane.cellCenterX(2), entryMidZ);
        assertNotNull(entrySeg, "expected rim→first-cell beam segment");
        assertEquals(0f, entrySeg.eulerY, 1e-4f);
        assertEquals(0.5f, entrySeg.scaleZ, 1e-4f);
    }

    @Test
    void stickyTestFireRetracesAfterPlace() {
        PuzzleMode puzzle = openBudget1();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        presentation.update(input, 800, 600);
        presentation.render(services.draw2d(), null, 800, 600);

        float margin = BoardPlane.portStripMargin(6);
        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(2), -margin * 0.5f));
        tap(input, presentation, 200, 200);

        thin3d.clear();
        tap(input, presentation, ChromeOverlay.trayForwardCenterX(), ChromeOverlay.trayCenterY());
        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(2), BoardPlane.cellCenterZ(0)));
        tap(input, presentation, 100, 100);
        presentation.render(services.draw2d(), null, 800, 600);

        assertEquals(PieceType.FORWARD, puzzle.pieceAt(2, 0));
        assertTrue(containsKind(thin3d.commands(), FakeThin3D.Command.Kind.INSTANCE_TRS));
        assertNotNull(
                findTrsNear(
                        thin3d.commands(),
                        BoardPlane.cellCenterX(2),
                        (0f + BoardPlane.cellCenterZ(0)) * 0.5f));
    }

    @Test
    void testFireTracesMergedBoardIncludingFixed() {
        PuzzleMode puzzle = openWithFixedForwardAt20();
        assertEquals(PieceType.FORWARD, puzzle.pieceAt(2, 0));
        assertTrue(puzzle.isFixed(2, 0));
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        presentation.update(input, 800, 600);
        presentation.render(services.draw2d(), null, 800, 600);
        thin3d.clear();

        float margin = BoardPlane.portStripMargin(6);
        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(2), -margin * 0.5f));
        tap(input, presentation, 200, 200);
        presentation.render(services.draw2d(), null, 800, 600);

        // Fixed / at (2,0) deflects T2 toward L0 — exit rim on left, not bottom B2.
        float leftExitMidX = (0f + BoardPlane.cellCenterX(0)) * 0.5f;
        assertNotNull(
                findTrsNear(thin3d.commands(), leftExitMidX, BoardPlane.cellCenterZ(0)),
                "merged Test Fire should deflect off fixed FORWARD");
    }

    @Test
    void tapFixedCellDoesNotRemove() {
        PuzzleMode puzzle = openWithFixedForwardAt20();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(2), BoardPlane.cellCenterZ(0)));
        tap(input, presentation, 100, 100);

        assertEquals(PieceType.FORWARD, puzzle.pieceAt(2, 0));
        assertEquals(1, puzzle.remaining());
    }

    @Test
    void orbTraySlotSelectsAndPlacesOrb() {
        PuzzleMode puzzle = openWithOrb();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();
        FakeThin3D thin3d = services.thin3d();

        ChromeOverlay.Layout layout = ChromeOverlay.Layout.forPuzzle(800, 600, puzzle);
        tap(input, presentation, layout.trayOrbCenterX(0), layout.trayCenterY());

        thin3d.seedPickRay(downwardOnto(BoardPlane.cellCenterX(0), BoardPlane.cellCenterZ(0)));
        tap(input, presentation, 100, 100);

        assertEquals(PieceType.ORB_1, puzzle.pieceAt(0, 0));
        assertEquals(0, puzzle.remainingOrb(PieceType.ORB_1));
    }

    @Test
    void levelWithOrbColorsSetsBoardPalette() {
        PuzzleMode puzzle = openWithOrb();
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);

        assertEquals(OrbColorPalette.Id.RED_YELLOW_BLUE, presentation.colorPalette().id());
    }

    @Test
    void chromeLayoutTracksScreenSize() {
        PuzzleMode puzzle = openBudget1();
        ChromeOverlay.Layout tall = ChromeOverlay.Layout.forPuzzle(800, 900, puzzle);
        ChromeOverlay.Layout wide = ChromeOverlay.Layout.forPuzzle(1280, 600, puzzle);
        assertTrue(tall.trayTop > ChromeOverlay.Layout.forPuzzle(800, 600, puzzle).trayTop);
        assertTrue(wide.hudLeft > ChromeOverlay.Layout.forPuzzle(800, 600, puzzle).hudLeft);
        assertEquals(
                ChromeOverlay.ChromeHit.TRAY_FORWARD,
                ChromeOverlay.hitTest(
                        tall.trayForwardCenterX(),
                        tall.trayCenterY(),
                        tall.screenWidth,
                        tall.screenHeight,
                        puzzle));
        assertEquals(
                ChromeOverlay.ChromeHit.NONE,
                ChromeOverlay.hitTest(10, 10, 800, 600, puzzle));
    }

    @Test
    void vKeyVerifiesWhenReady() {
        PuzzleMode puzzle = openBudget1();
        assertEquals(PlaceResult.OK, puzzle.place(2, 2, PieceType.FORWARD));
        FakeEngineServices services = new FakeEngineServices();
        Presentation presentation = pitched(puzzle, services);
        FakeInput input = services.input();

        input.setKeyDown(Key.V, true);
        presentation.update(input, 800, 600);

        assertTrue(presentation.lastVerifyResult().isOk());
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
