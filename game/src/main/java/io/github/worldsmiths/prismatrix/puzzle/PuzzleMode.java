package io.github.worldsmiths.prismatrix.puzzle;

import io.github.worldsmiths.core.Files;
import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Puzzle session: loaded level + mutable {@link DraftBoard}, with Verify against {@link TraceCore}.
 */
public final class PuzzleMode {

    private final LevelDefinition level;
    private final DraftBoard draft;

    private PuzzleMode(LevelDefinition level) {
        this.level = level;
        this.draft = new DraftBoard(level.gridSize(), level.mirrorBudget());
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

    /** Mutable draft board used by Verify; Ghost promote writes here. */
    public DraftBoard draft() {
        return draft;
    }

    public List<ClueDefinition> clues() {
        return level.clues();
    }

    public int remaining() {
        return draft.remaining();
    }

    public int mirrorBudget() {
        return draft.mirrorBudget();
    }

    public int gridSize() {
        return draft.gridSize();
    }

    /** Piece at cell, or {@code null} if empty / out of bounds. */
    public PieceType pieceAt(int x, int y) {
        return draft.pieceAt(x, y);
    }

    public PlaceResult place(int x, int y, PieceType type) {
        return draft.place(x, y, type);
    }

    public boolean remove(int x, int y) {
        return draft.remove(x, y);
    }

    /** True iff remaining mirror budget is 0 (Verify enabled). */
    public boolean canVerify() {
        return draft.remaining() == 0;
    }

    /**
     * When remaining &gt; 0, returns {@link VerifyResult#refused()} (not a FAIL). When remaining ==
     * 0, snapshots the board, traces each clue entry port, and compares singular exitPort +
     * colorMask.
     */
    public VerifyResult verify() {
        if (draft.remaining() > 0) {
            return VerifyResult.refused();
        }
        BoardState board = draft.toBoardState();
        List<Port> mismatched = new ArrayList<Port>();
        for (ClueDefinition clue : level.clues()) {
            TraceResult traced = TraceCore.trace(board, clue.entryPort());
            if (!matches(clue, traced)) {
                mismatched.add(clue.entryPort());
            }
        }
        if (mismatched.isEmpty()) {
            return VerifyResult.ok();
        }
        return VerifyResult.fail(mismatched);
    }

    private static boolean matches(ClueDefinition clue, TraceResult traced) {
        Port exit = traced.exitPort();
        if (exit == null) {
            return false;
        }
        return exit.equals(clue.exitPort()) && traced.colorMask() == clue.colorMask();
    }
}
