package io.github.worldsmiths.prismatrix.menu;

import io.github.worldsmiths.core.ui.UiKit;

/** Pure builder for the hub tray (Puzzle / PvP / Settings). Leaves filled in T2. */
public final class MenuHub {

    private MenuHub() {}

    /** Builds hub leaves into {@code kit}. T1 shell: no-op. */
    public static void build(UiKit kit) {
        if (kit == null) {
            throw new NullPointerException("kit");
        }
    }
}
