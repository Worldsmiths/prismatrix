package io.github.worldsmiths.prismatrix.pvp;

import io.github.worldsmiths.prismatrix.optics.BoardState;
import java.util.Random;

/**
 * Seat policy for AI opponents. Consumes sanitized {@link MatchView} only — never opponent secret
 * {@link BoardState}. Setup and play verbs go through the same kernel Ready / Fire / Guess paths
 * as a human.
 */
public interface AiPolicy {

    /** Random legal Classic placement for {@link MatchKernel#ready}. */
    BoardState proposeSetup(MatchRules rules, Random random);

    /**
     * Choose Fire XOR Guess from the sanitized seat view. Caller must only invoke on this policy's
     * active turn.
     */
    AiAction decide(MatchView view, Random random);
}
