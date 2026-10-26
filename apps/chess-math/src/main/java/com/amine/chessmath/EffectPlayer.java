package com.amine.chessmath;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

/** One-shot cues. Unready or denied cues are discarded, never queued. */
public final class EffectPlayer implements AutoCloseable {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AudioManager audio;
    private final AudioAttributes attributes=new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
    private final SoundPool pool=new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attributes).build();
    private final EnumMap<PresentationEvent.Cue,Integer> samples=new EnumMap<>(PresentationEvent.Cue.class);
    private final Set<Integer> ready=new HashSet<>();
    private AudioFocusRequest focus;
    private int stream,priority;
    private long until,generation;
    private boolean closed;
    public EffectPlayer(Context context){
        audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        pool.setOnLoadCompleteListener((p,id,status)->{if(!closed&&status==0)ready.add(id);});
        samples.put(PresentationEvent.Cue.MOVE,pool.load(context,R.raw.wood_move,1));
        samples.put(PresentationEvent.Cue.CAPTURE,pool.load(context,R.raw.wood_capture,1));
        samples.put(PresentationEvent.Cue.CHECK,pool.load(context,R.raw.gentle_check,1));
        samples.put(PresentationEvent.Cue.MATH_SUCCESS,pool.load(context,R.raw.math_success,1));
    }
    public void play(PresentationEvent.Cue cue){
        if(closed||!ready.contains(samples.get(cue)))return;
        int rank=cue==PresentationEvent.Cue.MOVE?1:cue==PresentationEvent.Cue.CAPTURE?2:3;
        if(stream!=0&&SystemClock.elapsedRealtime()<until&&rank<priority)return;
        stop();
        final long token=generation;
        AudioFocusRequest request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(change->{if(change<0&&generation==token)stop();},handler).build();
        focus=request;
        if(audio.requestAudioFocus(request)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stop();return;}
        float gain=cue==PresentationEvent.Cue.CAPTURE?.55f:cue==PresentationEvent.Cue.MATH_SUCCESS?.50f:.45f;
        int duration=cue==PresentationEvent.Cue.MOVE?105:cue==PresentationEvent.Cue.CAPTURE?145:cue==PresentationEvent.Cue.CHECK?236:265;
        stream=pool.play(samples.get(cue),gain,gain,rank,0,1);priority=rank;
        until=SystemClock.elapsedRealtime()+duration;
        handler.postDelayed(()->{if(generation==token)stop();},duration+15);
    }
    public void stop(){
        generation++;handler.removeCallbacksAndMessages(null);
        if(stream!=0){pool.stop(stream);stream=0;}
        if(focus!=null){audio.abandonAudioFocusRequest(focus);focus=null;}
        until=0;priority=0;
    }
    @Override public void close(){if(closed)return;stop();closed=true;pool.release();ready.clear();}
}
