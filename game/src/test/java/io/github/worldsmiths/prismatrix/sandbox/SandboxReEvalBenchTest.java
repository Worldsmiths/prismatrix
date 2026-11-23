package io.github.worldsmiths.prismatrix.sandbox;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.puzzle.PlacementBoard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Soft local latency harness for sandbox consistency re-eval at K≈20 observations (HLD NFR:
 * p95 &lt; 0.2 ms).
 *
 * <p>The soft p95 target is <strong>not</strong> asserted — CI must not flake on machine noise.
 * This test only fails on an extreme wall-clock timeout (orders of magnitude above 0.2 ms).
 *
 * <p>Peer of {@code TraceLatencyBenchTest}. Run focused locally:
 *
 * <pre>{@code
 * ./gradlew :game:test --tests io.github.worldsmiths.prismatrix.sandbox.SandboxReEvalBenchTest
 * }</pre>
 *
 * <p>Tagged {@code bench} so suites can include/exclude it via JUnit tags if desired. Default
 * {@code :game:test} still runs it; the extreme timeout keeps the suite non-flaky.
 */
@Tag("bench")
class SandboxReEvalBenchTest {

    private static final int BOARD_SIZE = 6;
    /** K≈20 observations re-evaluated per timed sample (showcase HLD NFR). */
    private static final int K = 20;
    private static final int WARMUP = 100;
    private static final int SAMPLES = 1000;
    /** Soft showcase target from HLD — recorded/printed, never a hard assert. */
    private static final double SOFT_P95_MS = 0.2;
    /**
     * Extreme wall-clock budget for the whole timed sample loop. Far above soft p95 × samples so
     * only pathological hangs fail CI.
     */
    private static final long EXTREME_WALL_MS = 30_000L;

    @Test
    void softK20SandboxReEval_recordsP95_extremeWallOnly() {
        GhostBoard ghosts = boardWithGhosts();
        List<ObservedShot> observations = twentyObservations();

        for (int i = 0; i < WARMUP; i++) {
            ConsistencyEngine.evaluate(ghosts, observations);
        }

        long[] nanos = new long[SAMPLES];
        long wallStart = System.nanoTime();
        for (int i = 0; i < SAMPLES; i++) {
            long t0 = System.nanoTime();
            ConsistencyEngine.evaluate(ghosts, observations);
            nanos[i] = System.nanoTime() - t0;
        }
        long wallMs = (System.nanoTime() - wallStart) / 1_000_000L;

        Arrays.sort(nanos);
        double p50Ms = nanos[SAMPLES / 2] / 1_000_000.0;
        double p95Ms = nanos[(int) (SAMPLES * 0.95)] / 1_000_000.0;
        double maxMs = nanos[SAMPLES - 1] / 1_000_000.0;

        System.out.printf(
                "SandboxReEvalBench K=%d: samples=%d p50=%.4f ms p95=%.4f ms max=%.4f ms"
                        + " wall=%d ms (soft p95 target < %.2f ms — not asserted)%n",
                K, SAMPLES, p50Ms, p95Ms, maxMs, wallMs, SOFT_P95_MS);

        assertTrue(
                wallMs < EXTREME_WALL_MS,
                "K="
                        + K
                        + " sandbox re-eval bench wall "
                        + wallMs
                        + " ms exceeded extreme budget "
                        + EXTREME_WALL_MS
                        + " ms (soft p95 target is informational only)");
    }

    /** 6×6 draft with a few ghosts so path∩ghost is non-trivial across ports. */
    private static GhostBoard boardWithGhosts() {
        PlacementBoard draft = PlacementBoard.mirrorsOnly(BOARD_SIZE, 4);
        GhostBoard ghosts = new GhostBoard(draft);
        ghosts.place(2, 1, PieceType.FORWARD);
        ghosts.place(3, 3, PieceType.BACK);
        ghosts.place(1, 4, PieceType.FORWARD);
        return ghosts;
    }

    /**
     * K=20 ObservedShots covering T/B/L/R ports (indices cycling within board size). Exit+color are
     * placeholders — bench times evaluate(), not badge correctness.
     */
    private static List<ObservedShot> twentyObservations() {
        Port.Side[] sides = {Port.Side.T, Port.Side.B, Port.Side.L, Port.Side.R};
        List<ObservedShot> shots = new ArrayList<ObservedShot>(K);
        for (int i = 0; i < K; i++) {
            Port.Side side = sides[i % sides.length];
            int index = i % BOARD_SIZE;
            Port entry = Port.of(side, index, BOARD_SIZE);
            Port exit = Port.of(Port.Side.B, index, BOARD_SIZE);
            shots.add(new ObservedShot(entry, exit, 0));
        }
        return shots;
    }
}
