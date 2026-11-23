package io.github.worldsmiths.prismatrix.puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.worldsmiths.core.FakeFiles;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * CI solved-oracle Verify: every packaged {@code assets/levels/w1/*.json} has a matching
 * classpath oracle under {@code oracles/w1/} that Verifies OK.
 */
class PackagedLevelOracleTest {

    private static final String ASSET_PREFIX = "levels/w1/";

    @Test
    void everyPackagedW1LevelHasSolvedOracleThatVerifiesOk() throws Exception {
        List<Path> levelFiles = listPackagedW1Levels();
        assertTrue(
                levelFiles.size() >= 5,
                "expected >= 5 packaged W1 levels, found " + levelFiles.size());

        for (Path levelFile : levelFiles) {
            String fileName = levelFile.getFileName().toString();
            String assetPath = ASSET_PREFIX + fileName;
            String levelJson = new String(Files.readAllBytes(levelFile), StandardCharsets.UTF_8);

            FakeFiles files = new FakeFiles();
            files.putInternal(assetPath, levelJson);

            PuzzleMode mode = PuzzleMode.load(files, assetPath);
            // Oracles are player placements only; applied after fixed seed on open.
            applyOracle(mode, fileName);

            assertEquals(0, mode.remaining(), "oracle must spend full budget for " + fileName);
            assertTrue(mode.canVerify(), "canVerify for " + fileName);

            VerifyResult result = mode.verify();
            assertFalse(result.isRefused(), "verify refused for " + fileName);
            assertTrue(
                    result.isOk(),
                    "oracle verify FAIL for "
                            + fileName
                            + " mismatched="
                            + result.mismatchedEntryPorts());
        }
    }

    private static void applyOracle(PuzzleMode mode, String levelFileName) throws Exception {
        String oracleResource = "oracles/w1/" + levelFileName;
        InputStream in = PackagedLevelOracleTest.class.getClassLoader().getResourceAsStream(oracleResource);
        if (in == null) {
            fail("Missing oracle resource: " + oracleResource);
        }
        String oracleJson;
        try {
            oracleJson = readFully(in);
        } finally {
            in.close();
        }

        LevelPack.MinimalJson.ObjectValue root = LevelPack.MinimalJson.parseObject(oracleJson);
        LevelPack.MinimalJson.ArrayValue placements = root.requireArray("placements");
        for (int i = 0; i < placements.size(); i++) {
            LevelPack.MinimalJson.ObjectValue p = placements.requireObject(i);
            int x = p.requireInt("x");
            int y = p.requireInt("y");
            String type = p.requireString("type");
            PieceType piece = parseMirrorType(type);
            PlaceResult placed = mode.place(x, y, piece);
            assertEquals(
                    PlaceResult.OK,
                    placed,
                    "oracle place failed for " + levelFileName + " at (" + x + "," + y + ")");
        }
    }

    private static PieceType parseMirrorType(String type) {
        if ("/".equals(type)) {
            return PieceType.FORWARD;
        }
        if ("\\".equals(type)) {
            return PieceType.BACK;
        }
        throw new IllegalArgumentException("oracle type must be \"/\" or \"\\\\\", got: " + type);
    }

    private static List<Path> listPackagedW1Levels() throws IOException {
        Path dir = resolveW1LevelsDir();
        List<Path> files = new ArrayList<Path>();
        DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json");
        try {
            for (Path p : stream) {
                files.add(p);
            }
        } finally {
            stream.close();
        }
        Collections.sort(files);
        return files;
    }

    private static Path resolveW1LevelsDir() {
        Path[] candidates =
                new Path[] {
                    Paths.get("assets", "levels", "w1"),
                    Paths.get("..", "assets", "levels", "w1")
                };
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        fail("Could not find assets/levels/w1 (cwd=" + Paths.get("").toAbsolutePath() + ")");
        return null;
    }

    private static String readFully(InputStream in) throws IOException {
        byte[] buf = new byte[4096];
        StringBuilder sb = new StringBuilder();
        int n;
        while ((n = in.read(buf)) >= 0) {
            sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
