package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import io.github.worldsmiths.prismatrix.optics.PieceType;
import io.github.worldsmiths.prismatrix.optics.Port;
import io.github.worldsmiths.prismatrix.optics.TraceCore;
import io.github.worldsmiths.prismatrix.optics.TraceResult;
import io.github.worldsmiths.prismatrix.sandbox.ObservedShot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Tactician tier: sample Classic layouts consistent with the firer shot log, Fire ports where
 * surviving hypotheses disagree on exit+color, and Guess when the Orbs Only orb triple is unique
 * under those constraints. MatchView-only — never opponent secret {@link BoardState}.
 */
public final class TacticianAiPolicy implements AiPolicy {

    /** Cap on kept consistent samples (greedy; not Master full search). */
    static final int HYPOTHESIS_SAMPLES = 24;

    /** Upper bound on random Classic draws while filtering against the log. */
    static final int SAMPLE_ATTEMPTS = 200;

    private final NoviceAiPolicy setup = new NoviceAiPolicy();

    @Override
    public BoardState proposeSetup(MatchRules rules, Random random) {
        return setup.proposeSetup(rules, random);
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
        MatchRules rules = MatchRules.builder().gridSize(gridSize).build();
        List<BoardState> consistent = sampleConsistent(view, rules, random);

        Integer guessesLeft = view.guessesLeft();
        boolean canGuess = guessesLeft == null || guessesLeft > 0;
        // Guess only after evidence: empty log never pins uniqueness under MatchView constraints.
        if (canGuess && !view.fireLog().isEmpty() && !consistent.isEmpty()) {
            BoardState unique = uniqueOrbHypothesis(consistent);
            if (unique != null) {
                return AiAction.guess(unique);
            }
        }

        List<Port> all = allPorts(gridSize);
        List<Port> unused = unusedPorts(view.fireLog(), all);
        List<Port> candidates = unused.isEmpty() ? all : unused;
        return AiAction.fire(preferDisagreementPort(consistent, candidates, random));
    }

    /**
     * Among {@code candidates}, pick a port maximizing distinct {@code (exit,color)} outcomes
     * across {@code hypotheses}. Package-visible for observation-aware Fire coverage.
     */
    static Port preferDisagreementPort(
            List<BoardState> hypotheses, List<Port> candidates, Random random) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        if (random == null) {
            throw new IllegalArgumentException("random must not be null");
        }
        if (hypotheses == null || hypotheses.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        int bestScore = -1;
        List<Port> best = new ArrayList<Port>();
        for (Port port : candidates) {
            int score = outcomeDiversity(hypotheses, port);
            if (score > bestScore) {
                bestScore = score;
                best.clear();
                best.add(port);
            } else if (score == bestScore) {
                best.add(port);
            }
        }
        return best.get(random.nextInt(best.size()));
    }

    private List<BoardState> sampleConsistent(MatchView view, MatchRules rules, Random random) {
        List<ObservedShot> log = view.fireLog();
        List<BoardState> kept = new ArrayList<BoardState>(HYPOTHESIS_SAMPLES);
        for (int attempt = 0;
                attempt < SAMPLE_ATTEMPTS && kept.size() < HYPOTHESIS_SAMPLES;
                attempt++) {
            BoardState candidate = proposeSetup(rules, random);
            if (consistentWithLog(candidate, log)) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    private static boolean consistentWithLog(BoardState board, List<ObservedShot> log) {
        for (ObservedShot shot : log) {
            TraceResult traced = TraceCore.trace(board, shot.entryPort());
            if (!Objects.equals(traced.exitPort(), shot.exitPort())
                    || traced.colorMask() != shot.colorMask()) {
                return false;
            }
        }
        return true;
    }

    /**
     * When every consistent sample shares the same Orbs Only orb triple, return one of them for
     * Guess; otherwise null.
     */
    private static BoardState uniqueOrbHypothesis(List<BoardState> consistent) {
        String key = orbKey(consistent.get(0));
        for (int i = 1; i < consistent.size(); i++) {
            if (!key.equals(orbKey(consistent.get(i)))) {
                return null;
            }
        }
        return consistent.get(0);
    }

    private static String orbKey(BoardState board) {
        StringBuilder sb = new StringBuilder(64);
        int n = board.size();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                PieceType piece = board.pieceAt(x, y);
                if (piece == PieceType.ORB_R
                        || piece == PieceType.ORB_G
                        || piece == PieceType.ORB_B) {
                    sb.append(piece.name()).append('@').append(x).append(',').append(y).append(';');
                }
            }
        }
        return sb.toString();
    }

    private static int outcomeDiversity(List<BoardState> hypotheses, Port port) {
        Set<Long> outcomes = new HashSet<Long>();
        for (BoardState hyp : hypotheses) {
            TraceResult traced = TraceCore.trace(hyp, port);
            outcomes.add(outcomeKey(traced.exitPort(), traced.colorMask()));
        }
        return outcomes.size();
    }

    private static long outcomeKey(Port exit, int colorMask) {
        long exitHash = exit == null ? 0L : (long) exit.hashCode();
        return (exitHash << 32) ^ (colorMask & 0xffffffffL) ^ (exit == null ? 1L : 0L);
    }

    private static List<Port> unusedPorts(List<ObservedShot> log, List<Port> all) {
        List<Port> unused = new ArrayList<Port>();
        for (Port port : all) {
            if (!alreadyFired(log, port)) {
                unused.add(port);
            }
        }
        return unused;
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
}
