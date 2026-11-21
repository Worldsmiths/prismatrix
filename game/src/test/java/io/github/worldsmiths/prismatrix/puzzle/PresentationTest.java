package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.core.FakeInput;
import io.github.worldsmiths.core.Key;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.Collections;
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

    @Test
    void hitTestMapsTopLeftCellToOpticsYZero() {
        Presentation presentation = new Presentation(openBudget1());
        int[] cell =
                presentation.hitTestCell(
                        (int) (Presentation.GRID_LEFT + 4), (int) (Presentation.GRID_TOP + 4));
        assertEquals(0, cell[0]);
        assertEquals(0, cell[1]);
    }

    @Test
    void hitTestMapsBottomRowToOpticsYFive() {
        Presentation presentation = new Presentation(openBudget1());
        float inputY =
                Presentation.GRID_TOP
                        + Presentation.CELL * 5
                        + Presentation.CELL / 2f;
        int[] cell = presentation.hitTestCell((int) (Presentation.GRID_LEFT + 4), (int) inputY);
        assertEquals(0, cell[0]);
        assertEquals(5, cell[1]);
    }

    @Test
    void trayThenEmptyCellPlacesSelectedMirror() {
        PuzzleMode puzzle = openBudget1();
        Presentation presentation = new Presentation(puzzle);
        FakeInput input = new FakeInput();

        tap(input, presentation, Presentation.trayForwardCenterX(), Presentation.trayCenterY());
        int cellX = (int) (Presentation.GRID_LEFT + Presentation.CELL / 2);
        int cellY = (int) (Presentation.GRID_TOP + Presentation.CELL / 2);
        tap(input, presentation, cellX, cellY);

        assertEquals(PieceType.FORWARD, puzzle.pieceAt(0, 0));
        assertEquals(0, puzzle.remaining());
    }

    @Test
    void tapFilledCellRemovesMirror() {
        PuzzleMode puzzle = openBudget1();
        assertEquals(PlaceResult.OK, puzzle.place(1, 1, PieceType.BACK));
        Presentation presentation = new Presentation(puzzle);
        FakeInput input = new FakeInput();

        int cellX = (int) (Presentation.GRID_LEFT + Presentation.CELL * 1.5f);
        int cellY = (int) (Presentation.GRID_TOP + Presentation.CELL * 1.5f);
        tap(input, presentation, cellX, cellY);

        assertNull(puzzle.pieceAt(1, 1));
        assertEquals(1, puzzle.remaining());
    }

    @Test
    void enterVerifiesWhenBudgetSpent() {
        PuzzleMode puzzle = openBudget1();
        assertEquals(PlaceResult.OK, puzzle.place(2, 2, PieceType.FORWARD));
        Presentation presentation = new Presentation(puzzle);
        FakeInput input = new FakeInput();

        input.setKeyDown(Key.ENTER, true);
        presentation.update(input, 800, 600);

        VerifyResult result = presentation.lastVerifyResult();
        assertFalse(result.isRefused());
        assertTrue(result.isOk());
    }

    @Test
    void enterDoesNothingWhileBudgetRemains() {
        PuzzleMode puzzle = openBudget1();
        Presentation presentation = new Presentation(puzzle);
        FakeInput input = new FakeInput();

        input.setKeyDown(Key.ENTER, true);
        presentation.update(input, 800, 600);

        assertNull(presentation.lastVerifyResult());
    }

    private static void tap(FakeInput input, Presentation presentation, int x, int y) {
        input.setPointer(x, y);
        input.setJustTouched(true);
        presentation.update(input, 800, 600);
        input.setJustTouched(false);
    }
}
