package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.prismatrix.optics.Port;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Outcome of {@link PuzzleMode#verify()}.
 *
 * <p>{@link #mismatchedEntryPorts()} is ordered by goal order in the level definition. Empty when
 * {@link #isOk()} or {@link #isRefused()}.
 */
public final class VerifyResult {

    private final boolean ok;
    private final boolean refused;
    private final List<Port> mismatchedEntryPorts;

    private VerifyResult(boolean ok, boolean refused, List<Port> mismatchedEntryPorts) {
        this.ok = ok;
        this.refused = refused;
        this.mismatchedEntryPorts =
                Collections.unmodifiableList(new ArrayList<Port>(mismatchedEntryPorts));
    }

    /** Verify was not run because remaining mirrors or orbs &gt; 0 (not a FAIL path). */
    public static VerifyResult refused() {
        return new VerifyResult(false, true, Collections.<Port>emptyList());
    }

    public static VerifyResult ok() {
        return new VerifyResult(true, false, Collections.<Port>emptyList());
    }

    public static VerifyResult fail(List<Port> mismatchedEntryPorts) {
        if (mismatchedEntryPorts == null) {
            throw new IllegalArgumentException("mismatchedEntryPorts must not be null");
        }
        return new VerifyResult(false, false, mismatchedEntryPorts);
    }

    public boolean isOk() {
        return ok;
    }

    /** True when verify was refused because remaining mirrors or orbs &gt; 0. */
    public boolean isRefused() {
        return refused;
    }

    /**
     * Entry ports whose Trace exitPort or colorMask did not match the goal. Ordered by goal order.
     * Empty when {@link #isOk()} or {@link #isRefused()}.
     */
    public List<Port> mismatchedEntryPorts() {
        return mismatchedEntryPorts;
    }
}
