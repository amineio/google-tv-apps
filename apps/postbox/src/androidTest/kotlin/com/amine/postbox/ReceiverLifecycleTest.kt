package com.amine.postbox

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.postbox.core.PairingLink
import com.amine.postbox.core.PostboxClient
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Controls dispatch order, not the native receiver or TLS implementation. */
@RunWith(AndroidJUnit4::class)
class ReceiverLifecycleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun staleStartJobsCannotCloseTheNewestReceiveWindow() {
        val fixture = Fixture()
        try {
            instrumentation.runOnMainSync {
                fixture.model.setResumed(true, true)
                fixture.model.enterAdd()
                fixture.model.permissionChanged(true)
            }
            assertEquals(3, fixture.io.size)
            // Run the latest IO job and all of its continuations before either older job.
            fixture.drainTail(2)
            val session = fixture.model.session ?: error("Newest receive window did not open.")
            fixture.drainTail(0)
            val paired = PostboxClient().pair(PairingLink.parse(session.pairingLink))
            assertEquals("Postbox TV", paired.name)
            assertEquals(session, fixture.model.session)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun queuedExpiryCleanupCannotCloseAReplacementReceiveWindow() {
        val fixture = Fixture()
        try {
            instrumentation.runOnMainSync {
                fixture.model.setResumed(true, true)
                fixture.model.enterAdd()
            }
            fixture.drainTail(0)
            val expired = fixture.model.session ?: error("Initial receive window did not open.")
            // Advance only the injected monotonic clock and release exactly one poll.
            fixture.now.addAndGet(300_001)
            fixture.poll.trySend(Unit).getOrThrow()
            instrumentation.waitForIdleSync()
            assertNull(fixture.model.session)
            assertEquals(1, fixture.io.size)
            instrumentation.runOnMainSync { fixture.model.retryReceiver() }
            assertEquals(2, fixture.io.size)
            // Keep the old cleanup queued while starting the replacement first.
            fixture.drainTail(1)
            val replacement = fixture.model.session ?: error("Replacement did not open.")
            assertNotEquals(expired.pairingLink, replacement.pairingLink)
            fixture.drainTail(0)
            val paired = PostboxClient().pair(PairingLink.parse(replacement.pairingLink))
            assertEquals("Postbox TV", paired.name)
            assertEquals(replacement, fixture.model.session)
        } finally {
            fixture.close()
        }
    }

    private inner class Fixture {
        val io = OrderedDispatcher()
        val now = AtomicLong(1000)
        val poll = Channel<Unit>(Channel.UNLIMITED)
        private val owner = ViewModelStore()
        lateinit var model: PostboxViewModel

        init {
            instrumentation.runOnMainSync {
                model =
                    PostboxViewModel(
                        instrumentation.targetContext.applicationContext as Application,
                        false,
                        receiverDispatcher = io,
                        receiverNowMs = now::get,
                        awaitReceiverPoll = { poll.receive() },
                    )
                owner.put("receiver-regression", model)
            }
        }

        fun drainTail(keep: Int) {
            while (true) {
                instrumentation.waitForIdleSync()
                if (io.size <= keep) return
                io.runAt(keep)
            }
        }

        fun close() {
            instrumentation.runOnMainSync { model.library() }
            drainTail(0)
            instrumentation.runOnMainSync { owner.clear() }
            poll.close()
            drainTail(0)
        }
    }

    private class OrderedDispatcher : CoroutineDispatcher() {
        private val queue = mutableListOf<Runnable>()
        val size: Int
            get() = synchronized(queue) { queue.size }

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            synchronized(queue) { queue.add(block) }
        }

        fun runAt(index: Int) {
            val next = synchronized(queue) { queue.removeAt(index) }
            next.run()
        }
    }
}
