package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.core.EngineException;
import io.github.worldsmiths.core.FakeFiles;
import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import org.junit.jupiter.api.Test;

class LevelPackTest {

    @Test
    void happyGoalsOnlyLoadPopulatesBudgetAndGoals() throws LevelLoadException {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w1/01.json",
                "{"
                        + "\"id\":\"w1-01\","
                        + "\"gridSize\":6,"
                        + "\"mirrorBudget\":4,"
                        + "\"goals\":["
                        + "{\"entryPort\":\"T2\",\"exitPort\":\"B2\",\"colorMask\":0}"
                        + "]"
                        + "}");

        LevelDefinition level = LevelPack.load(files, "levels/w1/01.json");

        assertEquals("w1-01", level.id());
        assertEquals(6, level.gridSize());
        assertEquals(4, level.mirrorBudget());
        assertEquals(0, level.orb1());
        assertEquals(0, level.orb2());
        assertEquals(0, level.orb3());
        assertNull(level.orbColors());
        assertTrue(level.fixedPieces().isEmpty());
        assertEquals(1, level.goals().size());
        GoalDefinition goal = level.goals().get(0);
        assertEquals(Port.of(Port.Side.T, 2, 6), goal.entryPort());
        assertEquals(Port.of(Port.Side.B, 2, 6), goal.exitPort());
        assertEquals(0, goal.colorMask());
        assertNotNull(goal.exitPort());
    }

    @Test
    void happyOrbsPaletteAndFixedPiecesLoad() throws LevelLoadException {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w2/orb-demo.json",
                "{"
                        + "\"id\":\"w2-orb-demo\","
                        + "\"gridSize\":6,"
                        + "\"mirrorBudget\":2,"
                        + "\"orb1\":1,"
                        + "\"orb2\":1,"
                        + "\"orb3\":0,"
                        + "\"orbColors\":\"RED_YELLOW_BLUE\","
                        + "\"goals\":["
                        + "{\"entryPort\":\"T2\",\"exitPort\":\"L2\",\"colorMask\":1}"
                        + "],"
                        + "\"fixedPieces\":["
                        + "{\"x\":1,\"y\":1,\"type\":\"/\"},"
                        + "{\"x\":2,\"y\":3,\"type\":\"ORB_1\"}"
                        + "]"
                        + "}");

        LevelDefinition level = LevelPack.load(files, "levels/w2/orb-demo.json");

        assertEquals(1, level.orb1());
        assertEquals(1, level.orb2());
        assertEquals(0, level.orb3());
        assertEquals(OrbColorPalette.Id.RED_YELLOW_BLUE, level.orbColors());
        assertEquals(2, level.fixedPieces().size());
        assertEquals(1, level.fixedPieces().get(0).x());
        assertEquals(1, level.fixedPieces().get(0).y());
        assertEquals(PieceType.FORWARD, level.fixedPieces().get(0).type());
        assertEquals(PieceType.ORB_1, level.fixedPieces().get(1).type());
    }

    @Test
    void missingGoalsFailsClosed() {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w1/no-goals.json",
                "{"
                        + "\"id\":\"w1-bad\","
                        + "\"gridSize\":6,"
                        + "\"mirrorBudget\":1,"
                        + "\"clues\":["
                        + "{\"entryPort\":\"T0\",\"exitPort\":\"B0\",\"colorMask\":0}"
                        + "]"
                        + "}");

        LevelLoadException ex =
                assertThrows(
                        LevelLoadException.class,
                        () -> LevelPack.load(files, "levels/w1/no-goals.json"));

        assertTrue(ex.getMessage().toLowerCase().contains("goals"));
    }

    @Test
    void orbsWithoutOrbColorsFailsClosed() {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w1/orbs-no-palette.json",
                "{"
                        + "\"id\":\"w1-bad\","
                        + "\"gridSize\":6,"
                        + "\"mirrorBudget\":1,"
                        + "\"orb1\":1,"
                        + "\"goals\":["
                        + "{\"entryPort\":\"T0\",\"exitPort\":\"B0\",\"colorMask\":0}"
                        + "]"
                        + "}");

        LevelLoadException ex =
                assertThrows(
                        LevelLoadException.class,
                        () -> LevelPack.load(files, "levels/w1/orbs-no-palette.json"));

        assertTrue(ex.getMessage().toLowerCase().contains("orbcolors"));
    }

    @Test
    void fixedPieceOutOfBoundsFailsClosed() {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w1/fixed-oob.json",
                "{"
                        + "\"id\":\"w1-bad\","
                        + "\"gridSize\":3,"
                        + "\"mirrorBudget\":0,"
                        + "\"goals\":["
                        + "{\"entryPort\":\"T0\",\"exitPort\":\"B0\",\"colorMask\":0}"
                        + "],"
                        + "\"fixedPieces\":[{\"x\":3,\"y\":0,\"type\":\"/\"}]"
                        + "}");

        assertThrows(
                LevelLoadException.class, () -> LevelPack.load(files, "levels/w1/fixed-oob.json"));
    }

    @Test
    void duplicateFixedPieceFailsClosed() {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w1/fixed-dupe.json",
                "{"
                        + "\"id\":\"w1-bad\","
                        + "\"gridSize\":3,"
                        + "\"mirrorBudget\":0,"
                        + "\"goals\":["
                        + "{\"entryPort\":\"T0\",\"exitPort\":\"B0\",\"colorMask\":0}"
                        + "],"
                        + "\"fixedPieces\":["
                        + "{\"x\":1,\"y\":1,\"type\":\"/\"},"
                        + "{\"x\":1,\"y\":1,\"type\":\"\\\\\"}"
                        + "]"
                        + "}");

        assertThrows(
                LevelLoadException.class, () -> LevelPack.load(files, "levels/w1/fixed-dupe.json"));
    }

    @Test
    void badFixedPieceTypeFailsClosed() {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w1/fixed-bad-type.json",
                "{"
                        + "\"id\":\"w1-bad\","
                        + "\"gridSize\":3,"
                        + "\"mirrorBudget\":0,"
                        + "\"goals\":["
                        + "{\"entryPort\":\"T0\",\"exitPort\":\"B0\",\"colorMask\":0}"
                        + "],"
                        + "\"fixedPieces\":[{\"x\":1,\"y\":1,\"type\":\"ABSORBER\"}]"
                        + "}");

        assertThrows(
                LevelLoadException.class,
                () -> LevelPack.load(files, "levels/w1/fixed-bad-type.json"));
    }

    @Test
    void missingPathFailsClosed() {
        FakeFiles files = new FakeFiles();

        LevelLoadException ex =
                assertThrows(
                        LevelLoadException.class,
                        () -> LevelPack.load(files, "levels/w1/missing.json"));

        assertTrue(ex.getMessage().contains("Missing"));
        assertTrue(ex.getCause() instanceof EngineException);
    }

    @Test
    void malformedJsonFailsClosed() {
        FakeFiles files = new FakeFiles();
        files.putInternal("levels/w1/bad.json", "{ not json");

        LevelLoadException ex =
                assertThrows(
                        LevelLoadException.class,
                        () -> LevelPack.load(files, "levels/w1/bad.json"));

        assertNotNull(ex.getMessage());
    }

    @Test
    void nullExitPortFailsClosed() {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w1/null-exit.json",
                "{"
                        + "\"id\":\"w1-bad\","
                        + "\"gridSize\":6,"
                        + "\"mirrorBudget\":1,"
                        + "\"goals\":["
                        + "{\"entryPort\":\"T0\",\"exitPort\":null,\"colorMask\":0}"
                        + "]"
                        + "}");

        LevelLoadException ex =
                assertThrows(
                        LevelLoadException.class,
                        () -> LevelPack.load(files, "levels/w1/null-exit.json"));

        assertTrue(ex.getMessage().toLowerCase().contains("exitport"));
    }

    @Test
    void missingExitPortFailsClosed() {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w1/no-exit.json",
                "{"
                        + "\"id\":\"w1-bad\","
                        + "\"gridSize\":6,"
                        + "\"mirrorBudget\":1,"
                        + "\"goals\":["
                        + "{\"entryPort\":\"T0\",\"colorMask\":0}"
                        + "]"
                        + "}");

        assertThrows(
                LevelLoadException.class, () -> LevelPack.load(files, "levels/w1/no-exit.json"));
    }
}
