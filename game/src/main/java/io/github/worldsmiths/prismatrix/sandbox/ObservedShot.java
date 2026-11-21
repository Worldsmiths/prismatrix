package io.github.worldsmiths.prismatrix.sandbox;

import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.puzzle.ClueDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Observation seam view: entry port, nullable exit port (absorb/cycle), color mask. Seeded from
 * level clues in M3; M4 Fire appends live observations (exit may be null).
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

    public static List<ObservedShot> fromClues(List<ClueDefinition> clues) {
        if (clues == null) {
            throw new IllegalArgumentException("clues must not be null");
        }
        List<ObservedShot> shots = new ArrayList<ObservedShot>(clues.size());
        for (ClueDefinition clue : clues) {
            shots.add(new ObservedShot(clue.entryPort(), clue.exitPort(), clue.colorMask()));
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
