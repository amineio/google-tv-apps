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
    private static final class Store implements PlayPreferences.Storage {
        private final Map<String,Boolean> values=new HashMap<>();
        boolean fail;
        public boolean read(String key,boolean fallback){return values.getOrDefault(key,fallback);}
        public boolean write(String key,boolean value){
            if(fail)return false;
            values.put(key,value);return true;
        }
    }
}
