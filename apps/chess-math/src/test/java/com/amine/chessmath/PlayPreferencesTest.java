package com.amine.chessmath;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class PlayPreferencesTest {
    @Test public void mathsDefaultsOnAndSuccessfulChoiceSurvivesRecreation() {
        Store store=new Store();PlayPreferences preferences=new PlayPreferences(store);
        assertTrue(preferences.mathsEnabled());assertTrue(preferences.setMathsEnabled(false));
        assertFalse(preferences.mathsEnabled());assertFalse(new PlayPreferences(store).mathsEnabled());
        assertTrue(preferences.setMathsEnabled(true));assertTrue(new PlayPreferences(store).mathsEnabled());
    }
    @Test public void failedChoiceKeepsLastPersistedMode() {
        Store store=new Store();PlayPreferences preferences=new PlayPreferences(store);store.fail=true;
        assertFalse(preferences.setMathsEnabled(false));assertTrue(preferences.mathsEnabled());
        assertTrue(new PlayPreferences(store).mathsEnabled());store.fail=false;
        assertTrue(preferences.setMathsEnabled(false));store.fail=true;
        assertFalse(preferences.setMathsEnabled(true));assertFalse(preferences.mathsEnabled());
        assertFalse(new PlayPreferences(store).mathsEnabled());
    }

    @Test public void thinkingDefaultsAndAllPresetsPersistWhileFailedChangesKeepPrevious() throws Exception {
        Store store=new Store();PlayPreferences p=new PlayPreferences(store);
        assertEquals(20,p.thinkingSeconds());
        for(int value:new int[]{0,5,10,20,30,60}){
            assertTrue(p.setThinkingSeconds(value));assertEquals(value,new PlayPreferences(store).thinkingSeconds());
        }
        store.fail=true;assertFalse(p.setThinkingSeconds(5));assertEquals(60,p.thinkingSeconds());
        assertEquals(60,new PlayPreferences(store).thinkingSeconds());
        for(int value:new int[]{-1,1,15,61}){
            try{p.setThinkingSeconds(value);fail("Invalid preset accepted");}
            catch(IllegalArgumentException expected){}
        }
    }
    @Test public void oldBooleanOnlyStorageDefaultsToTwentyAndRejectsUnstoredIntegerChoice(){
        PlayPreferences preferences=new PlayPreferences(new PlayPreferences.Storage(){
            public boolean read(String key,boolean fallback){return fallback;}
            public boolean write(String key,boolean value){return true;}
        });
        assertEquals(20,preferences.thinkingSeconds());assertFalse(preferences.setThinkingSeconds(5));
        assertEquals(20,preferences.thinkingSeconds());
    }
    private static final class Store implements PlayPreferences.Storage {
        private final Map<String,Boolean> values=new HashMap<>();
        private final Map<String,Integer> integers=new HashMap<>();
        public int readInt(String key,int fallback){return integers.getOrDefault(key,fallback);}
        public boolean writeInt(String key,int value){if(fail)return false;integers.put(key,value);return true;}
        boolean fail;
        public boolean read(String key,boolean fallback){return values.getOrDefault(key,fallback);}
        public boolean write(String key,boolean value){
            if(fail)return false;
            values.put(key,value);return true;
        }
    }
}
