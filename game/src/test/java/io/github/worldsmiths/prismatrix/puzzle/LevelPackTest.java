package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.core.EngineException;
import io.github.worldsmiths.core.FakeFiles;
import io.github.worldsmiths.prismatrix.optics.Port;
import org.junit.jupiter.api.Test;

class LevelPackTest {

    @Test
    void happyLoadPopulatesBudgetAndClues() throws LevelLoadException {
        FakeFiles files = new FakeFiles();
        files.putInternal(
                "levels/w1/01.json",
                "{"
                        + "\"id\":\"w1-01\","
                        + "\"gridSize\":6,"
                        + "\"mirrorBudget\":4,"
                        + "\"clues\":["
                        + "{\"entryPort\":\"T2\",\"exitPort\":\"B2\",\"colorMask\":0}"
                        + "]"
                        + "}");

        LevelDefinition level = LevelPack.load(files, "levels/w1/01.json");

        assertEquals("w1-01", level.id());
        assertEquals(6, level.gridSize());
        assertEquals(4, level.mirrorBudget());
        assertEquals(1, level.clues().size());
        ClueDefinition clue = level.clues().get(0);
        assertEquals(Port.of(Port.Side.T, 2, 6), clue.entryPort());
        assertEquals(Port.of(Port.Side.B, 2, 6), clue.exitPort());
        assertEquals(0, clue.colorMask());
        assertNotNull(clue.exitPort());
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
        // No Verify API exists yet — failed load must not return a level (fail closed, no session).
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
        // Fail closed before any Verify session (Verify API not present in M2 T1).
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
                        + "\"clues\":["
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
                        + "\"clues\":["
                        + "{\"entryPort\":\"T0\",\"colorMask\":0}"
                        + "]"
                        + "}");

        assertThrows(
                LevelLoadException.class, () -> LevelPack.load(files, "levels/w1/no-exit.json"));
    }
}
