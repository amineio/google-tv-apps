package com.gathin.tvhome;

import org.junit.Test;
import java.util.Collections;
import static org.junit.Assert.*;

public class StockHomePolicyTest {
    private static final String STOCK = "com.google.android.apps.tv.launcherx";
    private static final String HOME = STOCK + ".home.HomeActivity";

    @Test public void onlyTheConfiguredHomeWindowRedirects() {
        assertTrue(StockHomePolicy.redirect(true, STOCK, Collections.singleton(HOME), STOCK, HOME));
        assertFalse(StockHomePolicy.redirect(true, STOCK, Collections.singleton(HOME), STOCK, "SettingsActivity"));
        assertFalse(StockHomePolicy.redirect(true, STOCK, Collections.singleton(HOME), "com.google.android.apps.mediashell", HOME));
        assertFalse(StockHomePolicy.redirect(true, STOCK, Collections.singleton(HOME), "com.google.android.apps.mediashell", "CastWebContentsActivity"));
        assertFalse(StockHomePolicy.redirect(true, STOCK, Collections.singleton(HOME), null, null));
    }

    @Test public void restoringTheStockRoleStopsRedirecting() {
        assertFalse(StockHomePolicy.redirect(false, STOCK, Collections.singleton(HOME), STOCK, HOME));
        assertFalse(StockHomePolicy.redirect(true, "", Collections.singleton(HOME), STOCK, HOME));
    }

    @Test public void bootProbeRunsOnlyOnceAndOnlyNearBoot() {
        assertTrue(StockHomePolicy.probe(12, 11, 45000));
        assertFalse(StockHomePolicy.probe(12, 12, 45000));
        assertFalse(StockHomePolicy.probe(-1, -2, 45000));
        assertFalse(StockHomePolicy.probe(12, 11, 180000));
    }

    @Test public void longPressAndInvalidTimesAreNotTaps() {
        assertTrue(StockHomePolicy.shortPress(1000, 1120, 400));
        assertFalse(StockHomePolicy.shortPress(1000, 1400, 400));
        assertFalse(StockHomePolicy.shortPress(1000, 900, 400));
    }
}
