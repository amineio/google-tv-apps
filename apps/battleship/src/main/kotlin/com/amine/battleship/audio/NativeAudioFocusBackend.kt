package com.amine.battleship.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler

/** Each lease owns one native request. The controller invalidates callbacks before abandoning it. */
class NativeAudioFocusBackend(context: Context, private val handler: Handler) : AudioFocusBackend {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()

    override fun request(changed: (AudioFocusChange) -> Unit): AudioFocusLease {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes).setWillPauseWhenDucked(false)
            .setOnAudioFocusChangeListener({ event ->
                when (event) {
                    AudioManager.AUDIOFOCUS_GAIN -> changed(AudioFocusChange.GAIN)
                    AudioManager.AUDIOFOCUS_LOSS -> changed(AudioFocusChange.LOSS)
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> changed(AudioFocusChange.TRANSIENT_LOSS)
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> changed(AudioFocusChange.DUCK)
                }
            }, handler).build()
        val obtained = manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return object : AudioFocusLease {
            override val granted = obtained
            override fun abandon() { manager.abandonAudioFocusRequest(request) }
        }
    }
}
