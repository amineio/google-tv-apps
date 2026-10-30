package com.amine.chessmath;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class RecreationTest {
    @Test public void recreationReusesPendingWriterAndRestoresPaused() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Field shared=MainActivity.class.getDeclaredField("sharedController");
        shared.setAccessible(true);
        SessionController previous=(SessionController)shared.get(null);
        CountDownLatch release=new CountDownLatch(1);
        CountDownLatch writing=new CountDownLatch(1);
        SessionStore realStore=new SessionStore(instrumentation.getTargetContext().getFilesDir());
        SessionController.Persistence delayed=new SessionController.Persistence(){
            public Session load() throws Exception {return realStore.load();}
            public void save(Session session) throws Exception {
                writing.countDown();
                if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Test did not release save");
                realStore.save(session);
            }
        };
        Handler main=new Handler(Looper.getMainLooper());
        SessionController controller=new SessionController(delayed,SystemClock::elapsedRealtime,main::post,null);
        shared.set(null,controller);
        MainActivity first=null,recreated=null;
        Instrumentation.ActivityMonitor monitor=null;
        try {
            Intent intent=new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            first=(MainActivity)instrumentation.startActivitySync(intent);
            instrumentation.waitForIdleSync();
            MainActivity initial=first;
            instrumentation.runOnMainSync(()->{
                initial.getCurrentFocus().performClick();
                if(controller.state().revision==0&&!controller.busy())initial.getCurrentFocus().performClick();
            });
            assertTrue("Save must be in flight before recreation",writing.await(3,TimeUnit.SECONDS));
            assertTrue(controller.busy());
            long revision=controller.state().revision;
            java.util.List<String> history=new java.util.ArrayList<>(controller.state().moves);
            monitor=instrumentation.addMonitor(MainActivity.class.getName(),null,false);
            instrumentation.runOnMainSync(initial::recreate);
            recreated=(MainActivity)monitor.waitForActivityWithTimeout(5000);
            assertNotNull("Activity should be recreated",recreated);
            Field instance=MainActivity.class.getDeclaredField("controller");
            instance.setAccessible(true);
            assertSame("Recreation must not create a second AtomicFile writer",controller,instance.get(recreated));
            release.countDown();
            long until=SystemClock.elapsedRealtime()+5000;
            do { SystemClock.sleep(20);instrumentation.waitForIdleSync(); } while(controller.busy()&&SystemClock.elapsedRealtime()<until);
            assertFalse(controller.busy());
            assertTrue("Recreated session must remain paused",controller.state().paused);
            assertTrue(controller.state().revision>revision);
            assertEquals(history,controller.state().moves);
        } finally {
            release.countDown();
            if(monitor!=null)instrumentation.removeMonitor(monitor);
            MainActivity ending=recreated!=null?recreated:first;
            if(ending!=null)instrumentation.runOnMainSync(ending::finish);
            instrumentation.waitForIdleSync();
            controller.setListener(null);
            controller.close();
            shared.set(null,previous);
        }
    }
}
