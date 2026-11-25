package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.ui.UiKit;

/** Pure builder for Match settings. Leaves filled in T4. */
public final class MatchSettings {

    private MatchSettings() {}

    /** Builds Match settings leaves into {@code kit}. T1 shell: no-op. */
    public static void build(UiKit kit) {
        if (kit == null) {
            throw new NullPointerException("kit");
        }
    }
}
