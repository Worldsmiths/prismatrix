package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.prismatrix.optics.OrbColorPalette;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable loaded level: id, grid, budgets, goals, optional fixed décor and orb palette. */
public final class LevelDefinition {

    private final String id;
    private final int gridSize;
    private final int mirrorBudget;
    private final int orb1;
    private final int orb2;
    private final int orb3;
    private final OrbColorPalette.Id orbColors;
    private final List<GoalDefinition> goals;
    private final List<FixedPieceDefinition> fixedPieces;

    public LevelDefinition(
            String id,
            int gridSize,
            int mirrorBudget,
            List<GoalDefinition> goals) {
        this(
                id,
                gridSize,
                mirrorBudget,
                0,
                0,
                0,
                null,
                goals,
                Collections.<FixedPieceDefinition>emptyList());
    }

    public LevelDefinition(
            String id,
            int gridSize,
            int mirrorBudget,
            int orb1,
            int orb2,
            int orb3,
            OrbColorPalette.Id orbColors,
            List<GoalDefinition> goals,
            List<FixedPieceDefinition> fixedPieces) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (goals == null) {
            throw new IllegalArgumentException("goals must not be null");
        }
        if (fixedPieces == null) {
            throw new IllegalArgumentException("fixedPieces must not be null");
        }
        if (orb1 < 0 || orb2 < 0 || orb3 < 0) {
            throw new IllegalArgumentException("orb counts must be >= 0");
        }
        this.id = id;
        this.gridSize = gridSize;
        this.mirrorBudget = mirrorBudget;
        this.orb1 = orb1;
        this.orb2 = orb2;
        this.orb3 = orb3;
        this.orbColors = orbColors;
        this.goals = Collections.unmodifiableList(new ArrayList<GoalDefinition>(goals));
        this.fixedPieces =
                Collections.unmodifiableList(new ArrayList<FixedPieceDefinition>(fixedPieces));
    }

    public String id() {
        return id;
    }

    public int gridSize() {
        return gridSize;
    }

    public int mirrorBudget() {
        return mirrorBudget;
    }

    public int orb1() {
        return orb1;
    }

    public int orb2() {
        return orb2;
    }

    public int orb3() {
        return orb3;
    }

    /** Orb counts as {@code [orb1, orb2, orb3]}. */
    public int[] orbCounts() {
        return new int[] {orb1, orb2, orb3};
    }

    /** Palette id when the level declares orbs; {@code null} for mirrors-only levels. */
    public OrbColorPalette.Id orbColors() {
        return orbColors;
    }

    public List<GoalDefinition> goals() {
        return goals;
    }

    public List<FixedPieceDefinition> fixedPieces() {
        return fixedPieces;
    }
}
