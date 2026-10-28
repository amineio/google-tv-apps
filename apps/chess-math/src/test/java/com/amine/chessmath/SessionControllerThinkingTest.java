package com.amine.chessmath;

import org.junit.Test;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import static org.junit.Assert.*;

public class SessionControllerThinkingTest {
    private static final class Store implements SessionController.Persistence {
        Session saved; boolean fail;
        public Session load(){return saved.copy();}
        public void save(Session next) throws IOException {
            if(fail)throw new IOException("disk full");saved=next.copy();
        }
    }
    private static final class Fixture {
        long now; final Store store=new Store();
        final List<Runnable> computer=new ArrayList<>();
        SessionController c;
        Fixture(String... history){
            store.saved=new Session();store.saved.phase=Session.Phase.CHESS;
            store.saved.moves.addAll(Arrays.asList(history));recreate(Runnable::run);
        }
        void recreate(Executor io){
            if(c!=null)c.close();
            c=new SessionController(store,()->now,Runnable::run,io,computer::add,()->{});
        }
        void start(){c.foreground(true);c.resume();}
        void blackReply(){computer.remove(0).run();}
        void opening(){start();c.chooseSquare(52);c.chooseSquare(36);blackReply();}
    }
    private static long remaining(SessionController c){return c.thinkingRemainingMs();}
    private static long total(SessionController c){return c.thinkingTotalMs();}
    private static void duration(SessionController c,int seconds){c.setThinkingSeconds(seconds);}
    private static Session waitSave(Session session,long remaining,long total) throws Exception {
        JSONObject json=new JSONObject(SessionCodec.encode(session));
        json.put("thinkingRemainingMs",remaining).put("thinkingTotalMs",total);
        return SessionCodec.decode(json.toString());
    }
    @Test public void defaultReplyBlocksSelectionUntilExactlyTwentySeconds(){
        Fixture f=new Fixture();f.opening();
        f.c.chooseSquare(51);assertEquals(-1,f.c.state().selected);
        f.now=19999;f.c.tick();f.c.chooseSquare(51);assertEquals(-1,f.c.state().selected);
        assertEquals(1,remaining(f.c));f.now=20000;f.c.tick();
        f.c.chooseSquare(51);assertEquals(51,f.c.state().selected);assertEquals(0,remaining(f.c));
    }
    @Test public void openingWhiteMoveHasNoDelayAndBlackSnapshotCarriesFullDuration() throws Exception {
        Fixture f=new Fixture();f.start();assertEquals(0,remaining(f.c));
        f.c.chooseSquare(52);f.c.chooseSquare(36);assertEquals(0,remaining(f.c));
        f.blackReply();JSONObject saved=new JSONObject(SessionCodec.encode(f.store.saved));
        assertEquals(20000,saved.getLong("thinkingRemainingMs"));assertEquals(20000,total(f.c));
    }
    @Test public void presentationLandingStartsOnceAndStaleCallbacksDoNotRestart(){
        Fixture f=new Fixture("e2e4");List<PresentationEvent> events=new ArrayList<>();
        f.c.setPresenter(events::add);f.start();f.blackReply();PresentationEvent event=events.get(0);
        f.now=1000;assertEquals(20000,remaining(f.c));f.c.completePresentation(event.id);
        f.now=2500;assertEquals(18500,remaining(f.c));f.c.completePresentation(event.id);
        assertEquals(18500,remaining(f.c));f.c.restartGame();f.c.completePresentation(event.id);
        assertEquals(0,remaining(f.c));
    }
    @Test public void timeoutAndSynchronousLandingStartWithoutConsumingPresentationTime(){
        Fixture timeout=new Fixture("e2e4");timeout.c.setPresenter(e->{});timeout.start();timeout.blackReply();
        timeout.now=2000;timeout.c.tick();assertEquals(20000,remaining(timeout.c));
        timeout.now=2100;assertEquals(19900,remaining(timeout.c));
        Fixture sync=new Fixture("e2e4");sync.c.setPresenter(e->{sync.now+=500;sync.c.completePresentation(e.id);});
        sync.start();sync.blackReply();assertEquals(20000,remaining(sync.c));
        sync.now+=19999;assertEquals(1,remaining(sync.c));sync.now++;assertEquals(0,remaining(sync.c));
    }
    @Test public void interruptedPresentationAndBackgroundPreserveWait() {
        Fixture f=new Fixture("e2e4");f.c.setPresenter(e->{});f.start();f.blackReply();
        PresentationEvent.Id old=f.c.presentation().id;f.now=1500;f.c.foreground(false);
        assertEquals(20000,remaining(f.c));f.now=90000;f.c.completePresentation(old);
        f.c.foreground(true);f.c.resume();assertEquals(20000,remaining(f.c));
        f.now+=3000;f.c.pause();assertEquals(17000,remaining(f.c));
        f.now+=90000;f.c.resume();assertEquals(17000,remaining(f.c));
    }
    @Test public void recreationKeepsRemainingAndOriginalScale() throws Exception {
        Fixture f=new Fixture();f.opening();f.now=7000;f.c.pause();
        f.store.saved=SessionCodec.decode(SessionCodec.encode(f.store.saved));f.now=100000;
        f.recreate(Runnable::run);duration(f.c,5);assertTrue(f.c.state().paused);
        assertEquals(13000,remaining(f.c));assertEquals(20000,total(f.c));f.start();
        f.now+=12999;assertEquals(1,remaining(f.c));f.now++;assertEquals(0,remaining(f.c));
    }
    @Test public void slowCheckpointCountsNeitherSaveTimeNorPendingBackgroundTime(){
        Fixture f=new Fixture();f.opening();f.c.pause();List<Runnable> saves=new ArrayList<>();
        f.recreate(saves::add);f.start();saves.remove(0).run();f.now=5000;f.c.tick();
        assertTrue(f.c.busy());assertEquals(15000,remaining(f.c));
        f.now=25000;f.c.tick();assertNotNull(f.c.error());f.c.foreground(false);
        saves.remove(0).run();saves.remove(0).run();assertEquals(15000,remaining(f.c));
        f.now=50000;f.c.foreground(true);f.c.resume();saves.remove(0).run();
        assertEquals(15000,remaining(f.c));
    }
    @Test public void failedCheckpointPausesAtSettledTimeAndRetryDoesNotChargeErrorTime(){
        Fixture f=new Fixture();f.opening();f.store.fail=true;f.now=5000;f.c.tick();
        assertTrue(f.c.state().paused);assertNotNull(f.c.error());
        f.now=50000;f.store.fail=false;f.c.retrySave();assertEquals(15000,remaining(f.c));
        f.c.resume();f.now+=1000;assertEquals(14000,remaining(f.c));
    }
    @Test public void failedBlackSaveRetainsWhiteBoardThenRetryRestoresFullWait(){
        Fixture f=new Fixture();f.start();f.c.chooseSquare(52);f.c.chooseSquare(36);
        f.store.fail=true;f.blackReply();assertEquals(1,f.c.state().moves.size());
        f.now=90000;f.store.fail=false;f.c.retrySave();assertEquals(2,f.c.state().moves.size());
        assertEquals(20000,remaining(f.c));f.c.resume();assertEquals(20000,remaining(f.c));
    }
    @Test public void offClearsDurablyAndSuppressesStaleReloadEvenIfClearFailed(){
        Fixture f=new Fixture();f.opening();f.now=5000;duration(f.c,0);
        assertEquals(0,remaining(f.c));f.recreate(Runnable::run);f.start();assertEquals(0,remaining(f.c));
        Fixture failed=new Fixture();failed.opening();failed.store.fail=true;duration(failed.c,0);
        assertNotNull(failed.c.error());assertTrue(failed.c.state().paused);
        failed.recreate(Runnable::run);duration(failed.c,0);assertEquals(0,remaining(failed.c));
        failed.store.fail=false;failed.c.retrySave();failed.start();assertEquals(0,remaining(failed.c));
    }
    @Test public void nonzeroPreferenceOnlyAffectsNextReplyAndWhiteCommitClearsWait(){
        Fixture f=new Fixture();f.opening();f.now=4000;duration(f.c,5);
        assertEquals(16000,remaining(f.c));assertEquals(20000,total(f.c));
        f.now=20000;f.c.chooseSquare(51);f.c.chooseSquare(35);
        assertEquals(0,remaining(f.c));assertEquals(0,total(f.c));f.blackReply();
        assertEquals(5000,remaining(f.c));assertEquals(5000,total(f.c));
    }
    @Test public void parentSkipsPreserveBoardWaitAndOnlyChessTimeCounts(){
        Fixture f=new Fixture();f.opening();f.now=4000;f.c.skipToMath();
        assertEquals(16000,remaining(f.c));f.c.resume();f.now=100000;f.c.pause();
        assertEquals(16000,remaining(f.c));f.c.skipToChess();f.c.resume();
        f.now+=1000;assertEquals(15000,remaining(f.c));f.c.restartGame();assertEquals(0,remaining(f.c));
        f.c.chooseSquare(52);f.c.chooseSquare(36);f.blackReply();f.c.resetSession();assertEquals(0,remaining(f.c));
    }
    @Test public void intendedMoveAndPromotionCannotBypassWait() throws Exception {
        Fixture f=new Fixture();for(int i=0;i<2;i++)f.store.saved.moves.addAll(Arrays.asList("g1f3","g8f6","f3g1","f6g8"));
        f.store.saved.intendedMove="g1f3";f.store.saved=waitSave(f.store.saved,20000,20000);
        f.recreate(Runnable::run);f.start();f.c.playIntendedMove();assertEquals(8,f.c.state().moves.size());
        f.c.pause();f.c.playIntendedMove();assertEquals(8,f.c.state().moves.size());
        f.c.resume();f.c.claimDraw();assertEquals("Remise geclaimd",f.c.state().result);assertEquals(0,remaining(f.c));
        Fixture promotion=new Fixture("a2a4","h7h5","a4a5","h5h4","a5a6","h4h3","a6b7","h3g2");
        promotion.store.saved.selected=9;promotion.store.saved.promotionTo=0;
        promotion.store.saved=waitSave(promotion.store.saved,20000,20000);promotion.recreate(Runnable::run);
        promotion.start();promotion.c.promote('q');assertEquals(8,promotion.c.state().moves.size());
    }
    @Test public void terminalComputerMoveDoesNotCreateWait(){
        Fixture f=new Fixture("f2f3","e7e5","g2g4");f.start();f.blackReply();
        assertFalse(f.c.state().result.isEmpty());assertEquals(0,remaining(f.c));assertEquals(0,total(f.c));
    }
    @Test public void detachingPresenterStartsFullDelayWithoutReplaying(){
        Fixture f=new Fixture("e2e4");f.c.setPresenter(e->{});f.start();f.blackReply();
        f.now=1200;f.c.setPresenter(null);assertEquals(20000,remaining(f.c));
        f.now+=1000;assertEquals(19000,remaining(f.c));
    }
    @Test public void detachingAnIdlePresenterDoesNotResetSpentTime(){
        Fixture f=new Fixture();f.opening();f.now=4000;f.c.setPresenter(null);
        f.now=5000;assertEquals(15000,remaining(f.c));f.c.setListener(null);
        f.now=6000;assertEquals(14000,remaining(f.c));
    }
    @Test public void intendedMoveWaitCanExpireButHiddenOrPausedInputStillCannotPlay() throws Exception {
        Fixture f=new Fixture();for(int i=0;i<2;i++)f.store.saved.moves.addAll(Arrays.asList("g1f3","g8f6","f3g1","f6g8"));
        f.store.saved.intendedMove="g1f3";f.store.saved=waitSave(f.store.saved,5000,20000);
        f.recreate(Runnable::run);f.start();f.now=4999;f.c.playIntendedMove();
        assertEquals(8,f.c.state().moves.size());f.now=5000;assertEquals(0,remaining(f.c));
        f.c.foreground(false);f.c.playIntendedMove();assertEquals(8,f.c.state().moves.size());
        f.c.foreground(true);f.c.resume();f.c.playIntendedMove();assertEquals(9,f.c.state().moves.size());
        assertEquals(0,total(f.c));
    }
    @Test public void slowBlackSaveAndPausedLandingKeepFullDuration(){
        Fixture f=new Fixture("e2e4");List<Runnable> saves=new ArrayList<>();f.recreate(saves::add);
        f.start();saves.remove(0).run();f.blackReply();f.now=15000;f.c.tick();
        assertNotNull(f.c.error());f.c.pause();saves.remove(0).run();saves.remove(0).run();
        assertEquals(20000,remaining(f.c));assertTrue(f.c.state().paused);
    }
    @Test public void offDuringBusySaveClearsTheAcknowledgedSnapshot(){
        Fixture f=new Fixture();f.opening();f.c.pause();List<Runnable> saves=new ArrayList<>();
        f.recreate(saves::add);f.start();duration(f.c,0);assertEquals(0,remaining(f.c));
        saves.remove(0).run();if(!saves.isEmpty())saves.remove(0).run();
        assertFalse(f.c.busy());f.recreate(Runnable::run);assertEquals(0,remaining(f.c));
    }

    @Test public void failedPauseFreezesAlreadySpentTimeWithoutWaitingForRetry(){
        Fixture f=new Fixture();f.opening();f.now=3000;f.store.fail=true;f.c.pause();
        assertNotNull(f.c.error());assertEquals(17000,remaining(f.c));
        f.now=90000;assertEquals(17000,remaining(f.c));f.store.fail=false;
        f.c.retrySave();assertEquals(17000,remaining(f.c));
    }

}
