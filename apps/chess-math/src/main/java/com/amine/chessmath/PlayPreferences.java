package com.amine.chessmath;

import android.content.Context;
import android.content.SharedPreferences;

/** Play options are separate from the game snapshot and survive session resets. */
public final class PlayPreferences {
    interface Storage {
        boolean read(String key,boolean fallback);
        boolean write(String key,boolean value);
        default int readInt(String key,int fallback){return fallback;}
        default boolean writeInt(String key,int value){return false;}
    }
    private final Storage storage;
    private boolean mathsEnabled;
    private int thinkingSeconds;
    public PlayPreferences(Context context){this(new Storage(){
        private final SharedPreferences preferences=context.getSharedPreferences("play",Context.MODE_PRIVATE);
        public boolean read(String key,boolean fallback){return preferences.getBoolean(key,fallback);}
        public int readInt(String key,int fallback){return preferences.getInt(key,fallback);}
        @android.annotation.SuppressLint("ApplySharedPref") // Report durable write failure before applying the preference to play.
        public boolean writeInt(String key,int value){
            int previous=preferences.getInt(key,20);
            if(preferences.edit().putInt(key,value).commit())return true;
            preferences.edit().putInt(key,previous).commit();return false;
        }
        public boolean write(String key,boolean value){
            boolean previous=preferences.getBoolean(key,true);
            if(preferences.edit().putBoolean(key,value).commit())return true;
            // Restore the memory cache if the disk commit fails.
            preferences.edit().putBoolean(key,previous).commit();return false;
        }
    });}
    PlayPreferences(Storage storage){
        this.storage=storage;mathsEnabled=storage.read("maths_enabled",true);
        int saved=storage.readInt("thinking_seconds",20);
        thinkingSeconds=validThinkingSeconds(saved)?saved:20;
    }
    static boolean validThinkingSeconds(int value){
        return value==0||value==5||value==10||value==20||value==30||value==60;
    }
    public int thinkingSeconds(){return thinkingSeconds;}
    public boolean setThinkingSeconds(int value){
        if(!validThinkingSeconds(value))throw new IllegalArgumentException("Invalid thinking time");
        if(!storage.writeInt("thinking_seconds",value))return false;
        thinkingSeconds=value;return true;
    }
    public boolean mathsEnabled(){return mathsEnabled;}
    public boolean setMathsEnabled(boolean value){
        if(!storage.write("maths_enabled",value))return false;
        mathsEnabled=value;return true;
    }
}
