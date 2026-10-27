package com.amine.chessmath;

import java.lang.reflect.Field;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** Keep memory fixtures independent of the family's persisted play mode. */
public final class PlayModeRule implements TestRule {
    @Override public Statement apply(Statement base,Description description){
        return new Statement(){
            @Override public void evaluate() throws Throwable {
                Field field=MainActivity.class.getDeclaredField("sharedPlay");field.setAccessible(true);
                Object previous=field.get(null);
                field.set(null,new PlayPreferences(new PlayPreferences.Storage(){
                    private boolean enabled=true;
                    public boolean read(String key,boolean fallback){return enabled;}
                    public boolean write(String key,boolean value){enabled=value;return true;}
                }));
                try {base.evaluate();} finally {field.set(null,previous);}
            }
        };
    }
}
