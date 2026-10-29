package com.amine.asktv

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** All entry points run on the main thread. Each request owns a callback generation. */
class SpeechController(private val context: Context) {
    private val main=Handler(Looper.getMainLooper())
    private var recognition: SpeechRecognizer?=null
    private var generation=0L
    private var closed=false
    private var ready=false
    private var completion: ((Boolean)->Unit)?=null
    private val audio=context.getSystemService(AudioManager::class.java)
    private var focus: AudioFocusRequest?=null
    private var tts: TextToSpeech?=null
    init {
        tts=TextToSpeech(context) { status -> main.post {
            if(!closed && status==TextToSpeech.SUCCESS) {
                val voices=tts?.voices.orEmpty().filter { !it.isNetworkConnectionRequired && "notInstalled" !in it.features.orEmpty() }
                val voice=voices.firstOrNull { it.locale.language==Locale.getDefault().language }
                    ?: voices.firstOrNull { it.locale.language=="en" } ?: voices.firstOrNull()
                if(voice!=null) { tts?.voice=voice; ready=true }
            }
        } }
        tts?.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener() {
            override fun onStart(utteranceId:String?) {}
            override fun onDone(utteranceId:String?) { end(utteranceId,true) }
            @Deprecated("Android callback") override fun onError(utteranceId:String?) { end(utteranceId,false) }
            override fun onError(utteranceId:String?,errorCode:Int) { end(utteranceId,false) }
            private fun end(id:String?,success:Boolean) { main.post {
                if(!closed && id==generation.toString()) { val callback=completion; completion=null; abandonFocus(); callback?.invoke(success) }
            } }
        })
    }
    fun recognize(onText:(String)->Unit,onError:(String)->Unit) {
        stop()
        if(closed || !SpeechRecognizer.isRecognitionAvailable(context)) { onError("unavailable"); return }
        val request=generation
        try {
            val recognizer=SpeechRecognizer.createSpeechRecognizer(context)
            recognition=recognizer
            recognizer.setRecognitionListener(object:RecognitionListener {
                override fun onReadyForSpeech(params:Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB:Float) {}
                override fun onBufferReceived(buffer:ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(results:Bundle?) {}
                override fun onEvent(eventType:Int,params:Bundle?) {}
                override fun onError(error:Int) {
                    if(request!=generation || closed) return
                    recognition?.destroy(); recognition=null
                    onError(if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "permission" else "recognition")
                }
                override fun onResults(results:Bundle?) {
                    if(request!=generation || closed) return
                    recognition?.destroy(); recognition=null
                    val text=results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
                    if(text.isNullOrBlank()) onError("recognition") else onText(text)
                }
            })
            recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false)
            })
        } catch (_:Exception) { recognition?.destroy(); recognition=null; onError("unavailable") }
    }
    fun speak(text:String,onEnd:(Boolean)->Unit) {
        stop()
        if(closed || !ready || text.isBlank()) { onEnd(false); return }
        val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val request=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change -> if(change==AudioManager.AUDIOFOCUS_LOSS || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) { val callback=completion; stop(); callback?.invoke(false) } }.build()
        if(audio.requestAudioFocus(request)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { onEnd(false); return }
        focus=request
        completion=onEnd
        // Android engines have a fixed input limit. The readable answer always remains complete.
        val spoken=text.take(TextToSpeech.getMaxSpeechInputLength())
        if(tts?.speak(spoken,TextToSpeech.QUEUE_FLUSH,null,generation.toString())!=TextToSpeech.SUCCESS) {
            completion=null; abandonFocus(); onEnd(false)
        }
    }
    private fun abandonFocus() { focus?.let { audio.abandonAudioFocusRequest(it) }; focus=null }
    fun stop() { generation++; completion=null; recognition?.cancel(); recognition?.destroy(); recognition=null; tts?.stop(); abandonFocus() }
    fun close() { stop(); closed=true; ready=false; tts?.shutdown(); tts=null }
}
