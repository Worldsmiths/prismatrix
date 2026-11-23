package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.Port;

/**
 * Observation seam view: entry port, nullable exit port (absorb/cycle), color mask. M4 Fire
 * appends live observations (exit may be null).
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
