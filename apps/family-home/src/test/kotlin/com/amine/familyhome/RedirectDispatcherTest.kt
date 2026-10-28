package com.amine.familyhome

import org.junit.Assert.*
import org.junit.Test

class RedirectDispatcherTest {
    @Test fun rapidExcludedLaunchGetsDeferredRedirectInsteadOfBeingDropped() {
        var now = 0L
        var opened = 0
        val tasks = mutableListOf<Pair<Long, () -> Unit>>()
        val redirect = RedirectDispatcher({ now }, { delay, task -> tasks.add(delay to task) }, { true }) { opened++ }
        redirect.request()
        tasks.removeAt(0).second()
        now = 100
        redirect.request()
        assertEquals(250L, tasks.single().first)
        now = 350
        tasks.removeAt(0).second()
        assertEquals(2, opened)
    }

    @Test fun queuedRedirectRechecksPolicyAndCoalescesRepeatedEvents() {
        var needed = true
        var opened = 0
        val tasks = mutableListOf<() -> Unit>()
        val redirect = RedirectDispatcher({ 0 }, { _, task -> tasks.add(task) }, { needed }) { opened++ }
        redirect.request(); redirect.request(); redirect.request()
        assertEquals(1, tasks.size)
        needed = false
        tasks.single()()
        assertEquals(0, opened)
    }
}
