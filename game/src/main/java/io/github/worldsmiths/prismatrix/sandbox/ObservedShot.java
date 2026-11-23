package io.github.worldsmiths.prismatrix.sandbox;

import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.puzzle.GoalDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Observation seam view: entry port, nullable exit port (absorb/cycle), color mask. Seeded from
 * level goals for legacy Ghost badges; M4 Fire appends live observations (exit may be null).
 */
public final class ObservedShot {

    private final Port entryPort;
    private final Port exitPort;
    private final int colorMask;

    /**
     * @param exitPort exit border port, or {@code null} on absorb/cycle
     */
    public ObservedShot(Port entryPort, Port exitPort, int colorMask) {
        if (entryPort == null) {
            throw new IllegalArgumentException("entryPort must not be null");
        }
        this.entryPort = entryPort;
        this.exitPort = exitPort;
        this.colorMask = colorMask;
    }

    /** Bridge until T3 drops Ghost seeding; builds observations from puzzle goals. */
    public static List<ObservedShot> fromClues(List<GoalDefinition> goals) {
        if (goals == null) {
            throw new IllegalArgumentException("goals must not be null");
        }
        List<ObservedShot> shots = new ArrayList<ObservedShot>(goals.size());
        for (GoalDefinition goal : goals) {
            shots.add(new ObservedShot(goal.entryPort(), goal.exitPort(), goal.colorMask()));
        }
        return Collections.unmodifiableList(shots);
    }

    public Port entryPort() {
        return entryPort;
    }

    public Port exitPort() {
        return exitPort;
    }

    public int colorMask() {
        return colorMask;
    }
}
