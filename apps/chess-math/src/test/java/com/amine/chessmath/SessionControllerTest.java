package com.amine.chessmath;

import org.junit.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import static org.junit.Assert.*;

public class SessionControllerTest {
    private static final class Store implements SessionController.Persistence {
        Session saved;
        boolean fail;
        public Session load() { return saved == null ? null : saved.copy(); }
        public void save(Session s) throws IOException {
            if (fail) throw new IOException("disk full");
            saved = s.copy();
        }
    }
    private static final class Fixture {
        long now;
        Store store = new Store();
        List<Runnable> computer = new ArrayList<>();
        SessionController c = controller(Runnable::run);
        SessionController controller(Executor io) {
            return new SessionController(store, () -> now, Runnable::run, io,
                    computer::add, () -> {});
        }
    }

    @Test public void cleanWhiteBoundaryHandsOffAtFiveMinutes() {
        Fixture f = new Fixture();
        assertTrue(f.c.state().paused);
        f.c.foreground(true); f.c.resume();
        f.now = 299999; f.c.tick();
        assertEquals(Session.Phase.CHESS, f.c.state().phase);
        f.now = 300000; f.c.tick();
        assertEquals(Session.Phase.TO_MATH, f.c.state().phase);
    }

    @Test public void selectedTurnWaitsAndCancelReachesStableBoundary() {
        Fixture f = new Fixture(); f.c.foreground(true); f.c.resume();
        f.c.chooseSquare(52);
        f.now = 300000; f.c.tick();
        assertEquals(Session.Phase.CHESS, f.c.state().phase);
        assertTrue(f.c.state().chessPending);
        f.c.cancelSelection();
        assertEquals(Session.Phase.TO_MATH, f.c.state().phase);
    }

    @Test public void exactBoundaryInputCannotSelectAnotherTurnOrStartAnotherExercise() {
        Fixture f=new Fixture(); f.c.foreground(true); f.c.resume();
        f.now=300000; f.c.chooseSquare(52);
        assertEquals(Session.Phase.TO_MATH,f.c.state().phase);
        assertEquals(-1,f.c.state().selected);
        f.c.resume();
        for(int i=0;i<3;i++){answer(f.c);if(i<2)f.c.nextExercise();}
        long exercise=f.c.state().exercise.id;
        f.now=600000;f.c.nextExercise();
        assertEquals(Session.Phase.TO_CHESS,f.c.state().phase);
        assertEquals(exercise,f.c.state().exercise.id);
    }

    @Test public void arrowDuringSaveRetainsLatestFocus() {
        Fixture f=new Fixture(); f.c.foreground(true);f.c.resume();f.c.close();
        List<Runnable> saves=new ArrayList<>();f.c=f.controller(saves::add);
        f.c.foreground(true);f.c.resume();saves.remove(0).run();
        f.c.chooseSquare(52);f.c.focusSquare(44);saves.remove(0).run();
        assertEquals(44,f.c.state().focus);
    }

    @Test public void promotionBoundaryWaitsForChoiceAndExactlyOneBlackReply() throws Exception {
        Fixture f=new Fixture();Session saved=new Session();
        saved.moves.addAll(java.util.Arrays.asList("a2a4","h7h5","a4a5","h5h4","a5a6","h4h3","a6b7","h3g2"));
        saved.selected=9;saved.promotionTo=0;saved.elapsedMs=299999;
        f.store.saved=SessionCodec.decode(SessionCodec.encode(saved));f.c.close();f.c=f.controller(Runnable::run);
        f.c.foreground(true);f.c.resume();f.now=1;f.c.tick();
        assertTrue(f.c.state().chessPending);
        assertEquals(Session.Phase.CHESS,f.c.state().phase);
        assertEquals(0,f.c.state().promotionTo);
        f.c.promote('q');assertEquals(9,f.c.state().moves.size());
        assertEquals(Session.Phase.CHESS,f.c.state().phase);
        assertEquals(1,f.computer.size());f.computer.get(0).run();
        assertEquals(10,f.c.state().moves.size());
        assertEquals(Session.Phase.TO_MATH,f.c.state().phase);
        f.computer.get(0).run();assertEquals(10,f.c.state().moves.size());
    }

    @Test public void blackTurnRestoresPausedAndResumesOneReply() {
        Fixture f=new Fixture();f.c.foreground(true);f.c.resume();
        f.c.chooseSquare(52);f.c.chooseSquare(36);f.c.close();
        f.c=f.controller(Runnable::run);
        assertTrue(f.c.state().paused);assertFalse(f.c.game().whiteToMove());
        f.c.foreground(true);f.c.resume();assertEquals(2,f.computer.size());
        f.computer.get(0).run();assertEquals(1,f.c.state().moves.size());
        f.computer.get(1).run();f.computer.get(1).run();
        assertEquals(2,f.c.state().moves.size());assertTrue(f.c.game().whiteToMove());
    }

    @Test public void parentMathOverridePreservesOwedBlackReply() {
        Fixture f=new Fixture();f.c.foreground(true);f.c.resume();
        f.c.chooseSquare(52);f.c.chooseSquare(36);f.c.pause();f.c.skipToMath();
        assertEquals(Session.Phase.MATH,f.c.state().phase);
        f.computer.get(0).run();assertEquals(1,f.c.state().moves.size());
        f.c.skipToChess();f.c.resume();f.computer.get(1).run();
        assertEquals(2,f.c.state().moves.size());assertTrue(f.c.game().whiteToMove());
    }

    @Test public void configurationChangeDoesNotReplaceCurrentExercise() throws Exception {
        Fixture f=new Fixture();f.c.foreground(true);f.c.skipToMath();f.c.resume();
        long id=f.c.state().exercise.id;int answer=f.c.state().exercise.answer();
        MathConfig onlyTableTwo=new MathConfig();onlyTableTwo.addition=false;onlyTableTwo.subtraction=false;
        java.util.Arrays.fill(onlyTableTwo.tables,false);onlyTableTwo.tables[2]=true;
        f.c.pause();f.c.saveConfig(onlyTableTwo);
        Session restored=SessionCodec.decode(SessionCodec.encode(f.c.state()));
        assertEquals(id,restored.exercise.id);assertEquals(answer,restored.exercise.answer());
        f.c.resume();answer(f.c);f.c.nextExercise();
        assertEquals("MULTIPLY",f.c.state().exercise.operation);assertEquals(2,f.c.state().exercise.b);
    }

    @Test public void currentAndIntendedClaimsSupportPlayCancelAndRecovery() throws Exception {
        Fixture f=new Fixture();Session saved=new Session();
        for(int i=0;i<2;i++)saved.moves.addAll(java.util.Arrays.asList("g1f3","g8f6","f3g1","f6g8"));
        f.store.saved=saved;f.c.close();f.c=f.controller(Runnable::run);
        f.c.foreground(true);f.c.resume();f.c.chooseSquare(62);f.c.chooseSquare(45);
        assertEquals("g1f3",f.c.state().intendedMove);assertEquals(8,f.c.state().moves.size());
        Session restored=SessionCodec.decode(SessionCodec.encode(f.c.state()));
        assertEquals("g1f3",restored.intendedMove);
        f.c.cancelSelection();assertNull(f.c.state().intendedMove);assertEquals(62,f.c.state().selected);
        f.c.chooseSquare(45);f.c.playIntendedMove();assertEquals(9,f.c.state().moves.size());
        f.c.close();f.store.saved=restored;f.c=f.controller(Runnable::run);
        f.c.claimDraw();assertEquals("Remise geclaimd",f.c.state().result);assertEquals(8,f.c.state().moves.size());
        f.c.close();f.store.saved=saved;f.c=f.controller(Runnable::run);
        assertTrue(f.c.game().claimableDraw());f.c.claimDraw();
        assertEquals("Remise geclaimd",f.c.state().result);
    }

    @Test public void idleMathCannotFinishAndDuplicateSubmitCannotEarnCredit() {
        Fixture f = new Fixture(); f.c.foreground(true); f.c.skipToMath(); f.c.resume();
        f.now = 300000; f.c.tick();
        assertEquals(Session.Phase.MATH, f.c.state().phase);
        assertEquals(0, f.c.state().correct);
        answer(f.c); f.c.submitAnswer();
        assertEquals(1, f.c.state().correct);
        assertEquals(Session.Phase.MATH, f.c.state().phase);
        f.c.nextExercise(); answer(f.c);
        f.c.nextExercise(); answer(f.c);
        assertEquals(Session.Phase.TO_CHESS, f.c.state().phase);
    }

    @Test public void threeQuickAnswersStillRequireFiveForegroundMinutes() {
        Fixture f = new Fixture(); f.c.foreground(true); f.c.skipToMath(); f.c.resume();
        for (int i=0;i<3;i++) { answer(f.c); if(i<2)f.c.nextExercise(); }
        assertEquals(Session.Phase.MATH, f.c.state().phase);
        f.now=300000; f.c.tick();
        assertEquals(Session.Phase.TO_CHESS, f.c.state().phase);
    }

    @Test public void backgroundAndRestartDoNotEarnHiddenTime() {
        Fixture f = new Fixture(); f.c.foreground(true); f.c.resume();
        f.now=10000; f.c.foreground(false);
        f.now=1000000; f.c.tick();
        assertEquals(10000, f.c.state().elapsedMs);
        SessionController restored = f.controller(Runnable::run);
        assertTrue(restored.state().paused);
        restored.foreground(true); restored.resume();
        f.now+=1000; restored.tick(); restored.pause();
        assertEquals(11000, restored.state().elapsedMs);
    }

    @Test public void staleComputerResultAfterPauseDoesNotMove() {
        Fixture f = new Fixture(); f.c.foreground(true); f.c.resume();
        f.c.chooseSquare(52); f.c.chooseSquare(36);
        assertEquals(1, f.c.state().moves.size());
        assertEquals(1, f.computer.size());
        f.c.pause(); f.computer.get(0).run();
        assertEquals(1, f.c.state().moves.size());
    }

    @Test public void storageFailureDoesNotCommitMoveAndRetryPreservesIt() {
        Fixture f = new Fixture(); f.c.foreground(true); f.c.resume();
        f.c.chooseSquare(52); f.store.fail=true; f.c.chooseSquare(36);
        assertEquals(0, f.c.state().moves.size());
        assertNotNull(f.c.error());
        f.store.fail=false; f.c.retrySave();
        assertEquals(1, f.c.state().moves.size());
        assertTrue(f.c.state().paused);
    }

    @Test public void pauseDuringSaveUsesFocusLossInstant() {
        Fixture f = new Fixture(); f.c.foreground(true); f.c.resume();
        List<Runnable> saves = new ArrayList<>();
        f.c.close(); f.c=f.controller(saves::add); f.c.foreground(true); f.c.resume();
        saves.remove(0).run();
        f.now=10000; f.c.chooseSquare(52);
        f.now=11000; f.c.foreground(false);
        f.now=100000; saves.remove(0).run();
        while(!saves.isEmpty()) saves.remove(0).run();
        assertTrue(f.c.state().paused);
        assertEquals(10000, f.c.state().elapsedMs);
    }

    private static void answer(SessionController c) {
        for(char digit:Integer.toString(c.state().exercise.answer()).toCharArray()) c.digit(digit-'0');
        c.submitAnswer();
    }
}
