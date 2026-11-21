package io.github.worldsmiths.prismatrix.puzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable loaded level: id, grid size, untyped mirror budget, clues. */
public final class LevelDefinition {

    private final String id;
    private final int gridSize;
    private final int mirrorBudget;
    private final List<ClueDefinition> clues;

    public LevelDefinition(
            String id, int gridSize, int mirrorBudget, List<ClueDefinition> clues) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (clues == null) {
            throw new IllegalArgumentException("clues must not be null");
        }
        this.id = id;
        this.gridSize = gridSize;
        this.mirrorBudget = mirrorBudget;
        this.clues = Collections.unmodifiableList(new ArrayList<ClueDefinition>(clues));
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

    public List<ClueDefinition> clues() {
        return clues;
    }
}
