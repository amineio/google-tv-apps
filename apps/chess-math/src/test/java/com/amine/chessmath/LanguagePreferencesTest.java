package com.amine.chessmath;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LanguagePreferencesTest {
    @Test public void preferenceReloadKeepsChoiceAndRejectedWriteKeepsPreviousLanguage(){
        final String[] saved={null};final boolean[] writable={true};
        LanguagePreferences.Storage store=new LanguagePreferences.Storage(){
            public String read(){return saved[0];}
            public boolean write(String value){if(!writable[0])return false;saved[0]=value;return true;}
        };
        LanguagePreferences first=new LanguagePreferences(store);
        assertEquals("nl",first.language());
        assertTrue(first.setLanguage("en"));
        LanguagePreferences restarted=new LanguagePreferences(store);
        assertEquals("en",restarted.language());
        writable[0]=false;
        assertFalse(restarted.setLanguage("nl"));
        assertEquals("en",restarted.language());
        assertEquals("en",new LanguagePreferences(store).language());
    }
}
