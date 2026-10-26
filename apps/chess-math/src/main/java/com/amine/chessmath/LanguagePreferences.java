package com.amine.chessmath;

import android.content.Context;
import android.content.SharedPreferences;

/** Language is independent of game progress and survives a progress reset. */
public final class LanguagePreferences {
    interface Storage { String read(); boolean write(String language); }
    private final Storage storage;
    private String language;
    public LanguagePreferences(Context context){this(new Storage(){
        private final SharedPreferences preferences=context.getSharedPreferences("language",Context.MODE_PRIVATE);
        public String read(){return preferences.getString("language","nl");}
        public boolean write(String value){
            String previous=read();
            if(preferences.edit().putString("language",value).commit())return true;
            preferences.edit().putString("language",previous).commit();return false;
        }
    });}
    LanguagePreferences(Storage storage){this.storage=storage;language="en".equals(storage.read())?"en":"nl";}
    public String language(){return language;}
    public boolean setLanguage(String value){
        if(!("en".equals(value)||"nl".equals(value)))throw new IllegalArgumentException("Unsupported language");
        if(!storage.write(value))return false;
        language=value;return true;
    }
}
