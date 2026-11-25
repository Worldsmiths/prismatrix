package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.ui.UiKit;

/** Pure builder for World Select. Leaves filled in T3. */
public final class WorldSelect {

    private WorldSelect() {}

    /** Builds World Select leaves into {@code kit}. T1 shell: no-op. */
    public static void build(UiKit kit) {
        if (kit == null) {
            throw new NullPointerException("kit");
        }
    }
}
