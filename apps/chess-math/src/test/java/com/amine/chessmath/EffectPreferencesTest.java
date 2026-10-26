package com.amine.chessmath;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;
public class EffectPreferencesTest {
    @Test public void defaultsAreIndependentAndFailedWriteRetainsPriorChoice(){
        Map<String,Boolean> values=new HashMap<>();final boolean[] fail={false};
        EffectPreferences.Storage store=new EffectPreferences.Storage(){
            public boolean read(String key,boolean fallback){return values.getOrDefault(key,fallback);}
            public boolean write(String key,boolean value){if(fail[0])return false;values.put(key,value);return true;}
        };
        EffectPreferences p=new EffectPreferences(store);assertTrue(p.sound());assertTrue(p.motion());
        assertTrue(p.setSound(false));assertTrue(p.motion());
        EffectPreferences restored=new EffectPreferences(store);assertFalse(restored.sound());assertTrue(restored.motion());
        fail[0]=true;assertFalse(restored.setMotion(false));assertTrue(restored.motion());assertFalse(restored.sound());
    }
}
