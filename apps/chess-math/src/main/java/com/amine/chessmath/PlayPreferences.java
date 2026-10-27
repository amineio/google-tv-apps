package com.amine.chessmath;

import android.content.Context;
import android.content.SharedPreferences;

/** Play options are separate from the game snapshot and survive session resets. */
public final class PlayPreferences {
    interface Storage {
        boolean read(String key,boolean fallback);
        boolean write(String key,boolean value);
    }
    private final Storage storage;
    private boolean mathsEnabled;
    public PlayPreferences(Context context){this(new Storage(){
        private final SharedPreferences preferences=context.getSharedPreferences("play",Context.MODE_PRIVATE);
        public boolean read(String key,boolean fallback){return preferences.getBoolean(key,fallback);}
        public boolean write(String key,boolean value){
            boolean previous=preferences.getBoolean(key,true);
            if(preferences.edit().putBoolean(key,value).commit())return true;
            // Restore the memory cache if the disk commit fails.
            preferences.edit().putBoolean(key,previous).commit();return false;
        }
    });}
    PlayPreferences(Storage storage){
        this.storage=storage;mathsEnabled=storage.read("maths_enabled",true);
    }
    public boolean mathsEnabled(){return mathsEnabled;}
    public boolean setMathsEnabled(boolean value){
        if(!storage.write("maths_enabled",value))return false;
        mathsEnabled=value;return true;
    }
}
