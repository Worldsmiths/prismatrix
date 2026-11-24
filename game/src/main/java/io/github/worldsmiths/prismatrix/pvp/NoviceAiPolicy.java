package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.puzzle.PlacementBoard;
import io.github.worldsmiths.prismatrix.pvp.ObservedShot;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Novice tier: random legal Classic setup, prefer unused-port Fire, rare random Guess when budget
 * remains. Ignores observation consistency depth.
 */
public final class NoviceAiPolicy implements AiPolicy {

    /** 1-in-N chance to Guess instead of Fire when budget allows. */
    static final int GUESS_DENOMINATOR = 20;

    @Override
    public BoardState proposeSetup(MatchRules rules, Random random) {
        if (rules == null) {
            throw new IllegalArgumentException("rules must not be null");
        }
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        int n = rules.gridSize();
        PlacementBoard board = PlacementBoard.classic(n);
        PieceType[] inventory =
                new PieceType[] {
                    PieceType.ORB_1,
                    PieceType.ORB_2,
                    PieceType.ORB_3,
                    randomMirror(random),
                    randomMirror(random),
                    randomMirror(random),
                    randomMirror(random)
                };

        List<int[]> cells = new ArrayList<int[]>(n * n);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                cells.add(new int[] {x, y});
            }
        }
        shuffle(cells, random);
        for (int i = 0; i < inventory.length; i++) {
            int[] cell = cells.get(i);
            board.place(cell[0], cell[1], inventory[i]);
        }
        return board.toBoardState();
    }

    @Override
    public AiAction decide(MatchView view, Random random) {
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        int gridSize = view.gridSize();
        Integer guessesLeft = view.guessesLeft();
        boolean canGuess = guessesLeft == null || guessesLeft > 0;
        if (canGuess && random.nextInt(GUESS_DENOMINATOR) == 0) {
            return AiAction.guess(proposeSetup(MatchRules.builder().gridSize(gridSize).build(), random));
        }
        return AiAction.fire(pickFirePort(view, random, gridSize));
    }

    private static Port pickFirePort(MatchView view, Random random, int gridSize) {
        List<Port> all = allPorts(gridSize);
        List<Port> unused = new ArrayList<Port>();
        for (Port port : all) {
            if (!alreadyFired(view.fireLog(), port)) {
                unused.add(port);
            }
        }
        List<Port> pool = unused.isEmpty() ? all : unused;
        return pool.get(random.nextInt(pool.size()));
    }

    private static boolean alreadyFired(List<ObservedShot> log, Port port) {
        for (ObservedShot shot : log) {
            if (shot.entryPort().equals(port)) {
                return true;
            }
        }
        return false;
    }

    private static List<Port> allPorts(int gridSize) {
        List<Port> ports = new ArrayList<Port>(gridSize * 4);
        for (Port.Side side : Port.Side.values()) {
            for (int i = 0; i < gridSize; i++) {
                ports.add(Port.of(side, i, gridSize));
            }
        }
        return ports;
    }

    private static PieceType randomMirror(Random random) {
        return random.nextBoolean() ? PieceType.FORWARD : PieceType.BACK;
    }

    private static void shuffle(List<int[]> cells, Random random) {
        for (int i = cells.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] tmp = cells.get(i);
            cells.set(i, cells.get(j));
            cells.set(j, tmp);
        }
    }
}
