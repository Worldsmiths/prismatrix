package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.ui.UiKit;

/** Pure builder for App Settings placeholder. Leaves filled in T3. */
public final class AppSettings {

    private AppSettings() {}

    /** Builds App Settings leaves into {@code kit}. T1 shell: no-op. */
    public static void build(UiKit kit) {
        if (kit == null) {
            throw new NullPointerException("kit");
        }
    }
}
