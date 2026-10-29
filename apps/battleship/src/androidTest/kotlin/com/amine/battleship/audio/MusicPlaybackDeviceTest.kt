package com.amine.battleship.audio

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import com.amine.battleship.viewmodel.SoundCue
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Native decoding and playback-state evidence. These tests do not establish audible quality. */
@RunWith(AndroidJUnit4::class)
class MusicPlaybackDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun everyBundledMusicTrackDecodesAndPlaysThroughMediaPlayer() {
        for (track in MusicTrack.entries) {
            val ready = CountDownLatch(1)
            var playback: NativeMusicPlayback? = null
            try {
                instrumentation.runOnMainSync {
                    playback = NativeMusicBackend(context).prepare(track, { ready.countDown() }, { }) as NativeMusicPlayback
                }
                assertTrue("$track must prepare", ready.await(10, TimeUnit.SECONDS))
                instrumentation.runOnMainSync {
                    val active = checkNotNull(playback)
                    assertTrue("$track must contain decoded audio", active.player.duration > 1000)
                    active.volume(.05f)
                    active.start()
                    assertTrue("$track must enter native playing state", active.player.isPlaying)
                }
            } finally {
                instrumentation.runOnMainSync { playback?.release() }
            }
        }
    }

    @Test fun realNativePlayerStopsOnMuteAndBackgroundWithoutReplayingOpening() {
        val players = mutableListOf<NativeMusicPlayback>()
        val prepared = java.util.concurrent.LinkedBlockingQueue<Unit>()
        val native = NativeMusicBackend(context)
        val observed = object : MusicBackend {
            override fun prepare(track: MusicTrack, ready: () -> Unit, done: () -> Unit): MusicPlayback =
                native.prepare(track, { ready(); prepared.offer(Unit) }, done).also { players += it as NativeMusicPlayback }
        }
        val music = MusicController(observed)
        val home = SessionState(loaded = true)
        val game = home.copy(screen = Screen.GAME, game = GameEngine.newGame(42))
        try {
            instrumentation.runOnMainSync {
                music.foreground(true)
                music.update(home)
            }
            assertNotNull(prepared.poll(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                assertTrue(players.last().player.isPlaying)
                music.update(home.copy(soundEnabled = false))
                assertReleased(players.last())
                music.cue(SoundCue.MATCH_START, game)
            }
            assertNotNull(prepared.poll(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync {
                assertTrue(players.last().player.isPlaying)
                music.foreground(false)
                assertReleased(players.last())
                music.foreground(true)
                music.update(game)
                assertEquals("The discarded opening must not be prepared again", 2, players.size)
                music.update(home)
            }
            assertNotNull(prepared.poll(10, TimeUnit.SECONDS))
            instrumentation.runOnMainSync { assertTrue(players.last().player.isPlaying) }
        } finally {
            instrumentation.runOnMainSync { music.close() }
        }
    }

    private fun assertReleased(playback: NativeMusicPlayback) {
        try {
            assertFalse(playback.player.isPlaying)
        } catch (_: IllegalStateException) {
            // MediaPlayer rejects queries after release. It cannot keep playing.
        }
    }
}
