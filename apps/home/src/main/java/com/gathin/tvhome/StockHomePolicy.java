package com.gathin.tvhome;

import java.util.Set;

/** Decisions shared by the window redirect and its device-independent regression tests. */
final class StockHomePolicy {
    private StockHomePolicy() {}

    static boolean redirect(boolean ownsHome, String stock, Set<String> activities,
                            CharSequence pkg, CharSequence cls) {
        return ownsHome && !stock.isEmpty() && pkg != null && stock.contentEquals(pkg)
                && cls != null && activities.contains(cls.toString());
    }

    static boolean probe(int boot, int previousBoot, long uptimeMs) {
        return boot >= 0 && boot != previousBoot && uptimeMs < 180_000L;
    }

    static boolean shortPress(long down, long up, long timeout) {
        return up >= down && up - down < timeout;
    }
}
