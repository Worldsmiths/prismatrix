package io.github.worldsmiths.prismatrix.optics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Soft local latency harness for Trace on an 8×8 board (HLD NFR: p95 &lt; 0.5 ms).
 *
 * <p>The soft p95 target is <strong>not</strong> asserted — CI must not flake on machine noise.
 * This test only fails on an extreme wall-clock timeout (orders of magnitude above 0.5 ms).
 *
 * <p>Run focused locally:
 *
 * <pre>{@code
 * ./gradlew :game:test --tests io.github.worldsmiths.prismatrix.optics.TraceLatencyBenchTest
 * }</pre>
 *
 * <p>Tagged {@code bench} so suites can include/exclude it via JUnit tags if desired. Default
 * {@code :game:test} still runs it; the extreme timeout keeps the suite non-flaky.
 */
@Tag("bench")
class TraceLatencyBenchTest {

    private static final int BOARD_SIZE = 8;
    private static final int WARMUP = 200;
    private static final int SAMPLES = 2000;
    /** Soft showcase target from HLD — recorded/printed, never a hard assert. */
    private static final double SOFT_P95_MS = 0.5;
    /**
     * Extreme wall-clock budget for the whole timed sample loop. Far above soft p95 × samples so
     * only pathological hangs fail CI.
     */
    private static final long EXTREME_WALL_MS = 30_000L;

    @Test
    void soft8x8TraceLatency_recordsP95_extremeWallOnly() {
        BoardState board = eightByEightWithMirrorsAndOrbs();
        Port entry = Port.of(Port.Side.T, 3, BOARD_SIZE);

        for (int i = 0; i < WARMUP; i++) {
            TraceCore.trace(board, entry);
        }

        long[] nanos = new long[SAMPLES];
        long wallStart = System.nanoTime();
        for (int i = 0; i < SAMPLES; i++) {
            long t0 = System.nanoTime();
            TraceCore.trace(board, entry);
            nanos[i] = System.nanoTime() - t0;
        }
        long wallMs = (System.nanoTime() - wallStart) / 1_000_000L;

        Arrays.sort(nanos);
        double p50Ms = nanos[SAMPLES / 2] / 1_000_000.0;
        double p95Ms = nanos[(int) (SAMPLES * 0.95)] / 1_000_000.0;
        double maxMs = nanos[SAMPLES - 1] / 1_000_000.0;

        System.out.printf(
                "TraceLatencyBench 8x8: samples=%d p50=%.4f ms p95=%.4f ms max=%.4f ms"
                        + " wall=%d ms (soft p95 target < %.2f ms — not asserted)%n",
                SAMPLES, p50Ms, p95Ms, maxMs, wallMs, SOFT_P95_MS);

        assertTrue(
                wallMs < EXTREME_WALL_MS,
                "8x8 Trace bench wall " + wallMs + " ms exceeded extreme budget " + EXTREME_WALL_MS
                        + " ms (soft p95 target is informational only)");
    }

    /** Mirrors + orbs only (no Splitter). Layout exercises bounce + color mix on 8×8. */
    private static BoardState eightByEightWithMirrorsAndOrbs() {
        return BoardState.builder(BOARD_SIZE)
                .place(2, 3, PieceType.FORWARD)
                .place(4, 3, PieceType.ORB_R)
                .place(5, 1, PieceType.BACK)
                .place(1, 5, PieceType.ORB_G)
                .place(6, 6, PieceType.ORB_B)
                .place(3, 4, PieceType.FORWARD)
                .build();
    }
}
