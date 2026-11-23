package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.Files;
import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Puzzle session: fixed seed board + mutable player {@link PlacementBoard}, Verify on the merge.
 */
public final class PuzzleMode {

    private final LevelDefinition level;
    private final BoardState seed;
    private final Set<Long> fixedCells;
    private final PlacementBoard playerBoard;

    private PuzzleMode(LevelDefinition level) {
        this.level = level;
        this.fixedCells = new HashSet<Long>();
        BoardState.Builder seedBuilder = BoardState.builder(level.gridSize());
        for (FixedPieceDefinition fixed : level.fixedPieces()) {
            seedBuilder.place(fixed.x(), fixed.y(), fixed.type());
            fixedCells.add(cellKey(fixed.x(), fixed.y()));
        }
        this.seed = seedBuilder.build();
        this.playerBoard =
                PlacementBoard.puzzle(level.gridSize(), level.mirrorBudget(), level.orbCounts());
    }

    public static PuzzleMode open(LevelDefinition level) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        return new PuzzleMode(level);
    }

    public static PuzzleMode load(Files files, String path) throws LevelLoadException {
        return open(LevelPack.load(files, path));
    }

    public LevelDefinition level() {
        return level;
    }

    /** Player placement board (excludes fixed seed). Used by legacy Ghost/Promote until T2. */
    public PlacementBoard playerBoard() {
        return playerBoard;
    }

    public List<GoalDefinition> goals() {
        return level.goals();
    }

    public int remaining() {
        return playerBoard.remainingMirrors();
    }

    public int remainingMirrors() {
        return playerBoard.remainingMirrors();
    }

    public int remainingOrb(PieceType type) {
        return playerBoard.remainingOrb(type);
    }

    public int remainingOrbs() {
        return playerBoard.remainingOrbs();
    }

    public int mirrorBudget() {
        return playerBoard.mirrorBudget();
    }

    public int gridSize() {
        return playerBoard.gridSize();
    }

    public boolean isFixed(int x, int y) {
        return fixedCells.contains(Long.valueOf(cellKey(x, y)));
    }

    /** Piece at cell from merge (fixed or player), or {@code null} if empty / OOB. */
    public PieceType pieceAt(int x, int y) {
        if (isFixed(x, y)) {
            return seed.pieceAt(x, y);
        }
        return playerBoard.pieceAt(x, y);
    }

    public PlaceResult place(int x, int y, PieceType type) {
        if (isFixed(x, y)) {
            return PlaceResult.OCCUPIED;
        }
        return playerBoard.place(x, y, type);
    }

    public boolean remove(int x, int y) {
        if (isFixed(x, y)) {
            return false;
        }
        return playerBoard.remove(x, y);
    }

    /** Fixed seed with player pieces overlaid. */
    public BoardState mergedBoard() {
        BoardState.Builder builder = BoardState.builder(gridSize());
        for (int y = 0; y < gridSize(); y++) {
            for (int x = 0; x < gridSize(); x++) {
                PieceType piece = pieceAt(x, y);
                if (piece != null) {
                    builder.place(x, y, piece);
                }
            }
        }
        return builder.build();
    }

    /** True iff remaining mirrors and remaining orbs are both 0. */
    public boolean canVerify() {
        return remainingMirrors() == 0 && remainingOrbs() == 0;
    }

    /**
     * When inventory remains, returns {@link VerifyResult#refused()}. Otherwise traces each goal
     * entry on {@link #mergedBoard()} and compares exitPort + colorMask.
     */
    public VerifyResult verify() {
        if (!canVerify()) {
            return VerifyResult.refused();
        }
        BoardState board = mergedBoard();
        List<Port> mismatched = new ArrayList<Port>();
        for (GoalDefinition goal : level.goals()) {
            TraceResult traced = TraceCore.trace(board, goal.entryPort());
            if (!matches(goal, traced)) {
                mismatched.add(goal.entryPort());
            }
        }
        if (mismatched.isEmpty()) {
            return VerifyResult.ok();
        }
        return VerifyResult.fail(mismatched);
    }

    private static boolean matches(GoalDefinition goal, TraceResult traced) {
        Port exit = traced.exitPort();
        if (exit == null) {
            return false;
        }
        return exit.equals(goal.exitPort()) && traced.colorMask() == goal.colorMask();
    }

    private static long cellKey(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }
}
