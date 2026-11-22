package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;

/**
 * Locked Classic match configuration.
 *
 * <p>{@code maxGuesses == null} means unlimited. Null or {@code 0} setup/match seconds means
 * untimed. Orb presentation uses {@link OrbColorPalette} (default Red/Yellow/Blue).
 */
public final class MatchRules {

    private final int gridSize;
    private final boolean guessMirrors;
    private final Integer maxGuesses;
    private final Integer setupSeconds;
    private final Integer matchSeconds;
    private final OrbColorPalette.Id orbColors;

    private MatchRules(
            int gridSize,
            boolean guessMirrors,
            Integer maxGuesses,
            Integer setupSeconds,
            Integer matchSeconds,
            OrbColorPalette.Id orbColors) {
        this.gridSize = gridSize;
        this.guessMirrors = guessMirrors;
        this.maxGuesses = maxGuesses;
        this.setupSeconds = setupSeconds;
        this.matchSeconds = matchSeconds;
        this.orbColors = orbColors == null ? OrbColorPalette.Id.RED_YELLOW_BLUE : orbColors;
    }

    /** Classic defaults: 8×8, Orbs Only, unlimited guesses, untimed, RYB colors. */
    public static MatchRules classic() {
        return new MatchRules(8, false, null, null, null, OrbColorPalette.Id.RED_YELLOW_BLUE);
    }

    public static Builder builder() {
        return new Builder();
    }

    public int gridSize() {
        return gridSize;
    }

    public boolean guessMirrors() {
        return guessMirrors;
    }

    /** Null means unlimited. */
    public Integer maxGuesses() {
        return maxGuesses;
    }

    /** Null or 0 means untimed setup. */
    public Integer setupSeconds() {
        return setupSeconds;
    }

    /** Null or 0 means untimed match. */
    public Integer matchSeconds() {
        return matchSeconds;
    }

    /** Presentation palette for orb/beam/exit colors. */
    public OrbColorPalette.Id orbColors() {
        return orbColors;
    }

    public OrbColorPalette orbColorPalette() {
        return OrbColorPalette.of(orbColors);
    }

    public boolean timedSetup() {
        return setupSeconds != null && setupSeconds > 0;
    }

    public boolean timedMatch() {
        return matchSeconds != null && matchSeconds > 0;
    }

    public static final class Builder {
        private int gridSize = 8;
        private boolean guessMirrors;
        private Integer maxGuesses;
        private Integer setupSeconds;
        private Integer matchSeconds;
        private OrbColorPalette.Id orbColors = OrbColorPalette.Id.RED_YELLOW_BLUE;

        public Builder gridSize(int gridSize) {
            this.gridSize = gridSize;
            return this;
        }

        public Builder guessMirrors(boolean guessMirrors) {
            this.guessMirrors = guessMirrors;
            return this;
        }

        public Builder maxGuesses(Integer maxGuesses) {
            this.maxGuesses = maxGuesses;
            return this;
        }

        public Builder setupSeconds(Integer setupSeconds) {
            this.setupSeconds = setupSeconds;
            return this;
        }

        public Builder matchSeconds(Integer matchSeconds) {
            this.matchSeconds = matchSeconds;
            return this;
        }

        public Builder orbColors(OrbColorPalette.Id orbColors) {
            this.orbColors = orbColors;
            return this;
        }

        public MatchRules build() {
            return new MatchRules(
                    gridSize, guessMirrors, maxGuesses, setupSeconds, matchSeconds, orbColors);
        }
    }
}
