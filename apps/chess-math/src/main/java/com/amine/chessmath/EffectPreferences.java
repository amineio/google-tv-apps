package com.amine.chessmath;

import android.content.Context;
import android.content.SharedPreferences;

/** Separate from the atomic game snapshot and preserved by game resets. */
public final class EffectPreferences {
    interface Storage {
        boolean read(String key,boolean fallback);
        boolean write(String key,boolean value);
    }
    private final Storage storage;
    private boolean sound,motion;
    public EffectPreferences(Context context){this(new Storage(){
        private final SharedPreferences preferences=context.getSharedPreferences("presentation",Context.MODE_PRIVATE);
        public boolean read(String key,boolean fallback){return preferences.getBoolean(key,fallback);}
        public boolean write(String key,boolean value){
            boolean previous=preferences.getBoolean(key,true);
            if(preferences.edit().putBoolean(key,value).commit())return true;
            // SharedPreferences updates its memory cache even when disk commit fails.
            preferences.edit().putBoolean(key,previous).commit();return false;
        }
    });}
    EffectPreferences(Storage storage){
        this.storage=storage;sound=storage.read("sound",true);motion=storage.read("motion",true);
    }
    public boolean sound(){return sound;}
    public boolean motion(){return motion;}
    public boolean setSound(boolean value){
        if(!storage.write("sound",value))return false;
        sound=value;return true;
    }
    public boolean setMotion(boolean value){
        if(!storage.write("motion",value))return false;
        motion=value;return true;
    }
}
