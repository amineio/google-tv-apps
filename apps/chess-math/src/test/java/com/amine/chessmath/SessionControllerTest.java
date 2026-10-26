package com.amine.chessmath;

import org.junit.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import static org.junit.Assert.*;

public class SessionControllerTest {
    @Test public void freshSessionStartsWithArithmeticAndValidExercise() {
        Session fresh=new Session();
        assertEquals(Session.Phase.MATH,fresh.phase);assertNotNull(fresh.exercise);
        Fixture f=new Fixture();f.c.close();f.store.saved=null;f.c=f.controller(Runnable::run);
        assertEquals(Session.Phase.MATH,f.c.state().phase);assertTrue(f.c.state().paused);
        f.c.foreground(true);f.c.resume();assertEquals(Session.Phase.MATH,f.c.state().phase);
    }
    @Test public void newChessStartsDirectlyFromPausedMath() {
        Fixture f=new Fixture();f.c.foreground(true);f.c.resume();
        f.c.skipToMath();
        assertTrue(f.c.state().paused);
        f.c.restartGame();
        assertEquals(Session.Phase.CHESS,f.c.state().phase);
        assertFalse(f.c.state().paused);
        assertEquals(0,f.c.state().elapsedMs);
        assertTrue(f.c.state().moves.isEmpty());
    }
    private static final class Store implements SessionController.Persistence {
        Session saved;
        boolean fail;
        public Session load() { return saved == null ? null : saved.copy(); }
        public void save(Session s) throws IOException {
            if (fail) throw new IOException("disk full");
            saved = s.copy();
        }
    }
    private static Store chessStore(){
        Store store=new Store();store.saved=new Session();store.saved.phase=Session.Phase.CHESS;return store;
    }
    private static final class Fixture {
        long now;
        Store store = chessStore();
        List<Runnable> computer = new ArrayList<>();
        SessionController c = controller(Runnable::run);
        SessionController controller(Executor io) {
            return new SessionController(store, () -> now, Runnable::run, io,
                    computer::add, () -> {});
        }
    }

    @Test public void chessDoesNotEndAtFiveMinutesOrLater() {
        Fixture f=new Fixture();f.c.foreground(true);f.c.resume();
        f.now=300000;f.c.tick();assertEquals(Session.Phase.CHESS,f.c.state().phase);
        assertFalse(f.c.state().chessPending);
        f.now=900000;f.c.chooseSquare(52);assertEquals(52,f.c.state().selected);
        f.c.cancelSelection();assertEquals(Session.Phase.CHESS,f.c.state().phase);
    }
    @Test public void mathFinishesCurrentExerciseBeforeStartingBreak() {
        Fixture f=mathWithAnswer(7);f.c.state().correct=3;
        f.now=300000;f.c.tick();assertEquals(Session.Phase.MATH,f.c.state().phase);
        f.c.digit(7);f.now+=1199;f.c.tick();assertEquals(Session.Phase.MATH,f.c.state().phase);
        f.now++;f.c.tick();assertEquals(Session.Phase.BREAK,f.c.state().phase);
        assertEquals(0,f.c.state().elapsedMs);assertFalse(f.c.state().paused);
        assertEquals(60000,f.c.breakRemainingMs());
    }

    @Test public void arrowDuringSaveRetainsLatestFocus() {
        Fixture f=new Fixture(); f.c.foreground(true);f.c.resume();f.c.close();
        List<Runnable> saves=new ArrayList<>();f.c=f.controller(saves::add);
        f.c.foreground(true);f.c.resume();saves.remove(0).run();
        f.c.chooseSquare(52);f.c.focusSquare(44);saves.remove(0).run();
        assertEquals(44,f.c.state().focus);
    }

    @Test public void promotionBoundaryWaitsForChoiceAndExactlyOneBlackReply() throws Exception {
        Fixture f=new Fixture();Session saved=new Session();saved.phase=Session.Phase.CHESS;
        saved.moves.addAll(java.util.Arrays.asList("a2a4","h7h5","a4a5","h5h4","a5a6","h4h3","a6b7","h3g2"));
        saved.selected=9;saved.promotionTo=0;saved.elapsedMs=299999;
        f.store.saved=SessionCodec.decode(SessionCodec.encode(saved));f.c.close();f.c=f.controller(Runnable::run);
        f.c.foreground(true);f.c.resume();f.now=1;f.c.tick();
        assertFalse(f.c.state().chessPending);
        assertEquals(Session.Phase.CHESS,f.c.state().phase);
        assertEquals(0,f.c.state().promotionTo);
        f.c.promote('q');assertEquals(9,f.c.state().moves.size());
        assertEquals(Session.Phase.CHESS,f.c.state().phase);
        assertEquals(1,f.computer.size());f.computer.get(0).run();
        assertEquals(10,f.c.state().moves.size());
        assertEquals(Session.Phase.CHESS,f.c.state().phase);
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
        Fixture f=new Fixture();Session saved=new Session();saved.phase=Session.Phase.CHESS;
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
        assertEquals(Session.Phase.MATH, f.c.state().phase);
        f.now+=1200;f.c.tick();
        assertEquals(Session.Phase.BREAK, f.c.state().phase);
    }

    @Test public void threeQuickAnswersStillRequireFiveForegroundMinutes() {
        Fixture f = new Fixture(); f.c.foreground(true); f.c.skipToMath(); f.c.resume();
        for (int i=0;i<3;i++) { answer(f.c); if(i<2)f.c.nextExercise(); }
        assertEquals(Session.Phase.MATH, f.c.state().phase);
        f.now=300000; f.c.tick();
        assertEquals(Session.Phase.BREAK, f.c.state().phase);
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

    private static Fixture mathWithAnswer(int value) {
        Fixture f=new Fixture();
        Session saved=new Session();saved.phase=Session.Phase.MATH;
        saved.exercise=new Exercise(123,value,0,Exercise.ADD);
        saved.exerciseConfig=saved.config.copy();
        f.store.saved=saved;f.c.close();f.c=f.controller(Runnable::run);
        f.c.foreground(true);f.c.resume();return f;
    }

    @Test public void numericEntrySolvesZeroOneTwoAndThreeDigitAnswersWithoutSubmit() {
        for(int value:new int[]{0,7,25,100}) {
            Fixture f=mathWithAnswer(value);
            String digits=Integer.toString(value);
            for(int i=0;i<digits.length();i++) {
                f.c.digit(digits.charAt(i)-'0');
                assertEquals(i==digits.length()-1,f.c.state().solved);
                if(!f.c.state().solved)assertEquals("",f.c.state().feedback);
            }
            assertEquals(1,f.c.state().correct);
            assertEquals("Goed gedaan!",f.c.state().feedback);
            f.c.digit(0);f.c.submitAnswer();assertEquals(1,f.c.state().correct);
            f.now=1199;f.c.tick();assertEquals(123,f.c.state().exercise.id);
            f.now=1200;f.c.tick();assertNotEquals(123,f.c.state().exercise.id);
            assertFalse(f.c.state().solved);assertEquals(1,f.c.state().correct);
        }
    }

    @Test public void wrongCompleteAnswerStaysEditableAndShortPrefixesHaveNoError() {
        Fixture f=mathWithAnswer(25);
        f.c.digit(2);assertEquals("",f.c.state().feedback);assertEquals(0,f.c.state().hintLevel);
        f.c.digit(4);assertEquals("Probeer nog eens.",f.c.state().feedback);
        assertEquals(0,f.c.state().hintLevel);
        assertFalse(f.c.state().solved);assertEquals(0,f.c.state().correct);
        f.c.deleteDigit();assertEquals("2",f.c.state().input);
        f.c.digit(5);assertTrue(f.c.state().solved);assertEquals(1,f.c.state().correct);
    }

    @Test public void pauseAndBackgroundRequireFreshVisibleSuccessDelay() {
        Fixture f=mathWithAnswer(7);f.c.digit(7);
        f.now=1000;f.c.pause();f.now=100000;f.c.tick();
        assertEquals(123,f.c.state().exercise.id);
        f.c.resume();f.now+=1199;f.c.tick();assertEquals(123,f.c.state().exercise.id);
        f.c.foreground(false);f.now+=100000;f.c.tick();assertEquals(123,f.c.state().exercise.id);
        f.c.foreground(true);f.c.resume();f.now+=1200;f.c.tick();
        assertNotEquals(123,f.c.state().exercise.id);assertEquals(1,f.c.state().correct);
    }

    @Test public void restoredSolvedExerciseGetsFreshDelayAndNoSecondCredit() {
        Fixture f=mathWithAnswer(7);f.c.digit(7);f.c.close();
        f.now=100000;f.c=f.controller(Runnable::run);f.c.foreground(true);
        f.c.tick();assertEquals(123,f.c.state().exercise.id);
        f.c.resume();f.c.digit(7);f.c.submitAnswer();
        f.now+=1199;f.c.tick();assertEquals(123,f.c.state().exercise.id);
        f.now++;f.c.tick();assertNotEquals(123,f.c.state().exercise.id);
        assertEquals(1,f.c.state().correct);
    }

    @Test public void delayedSaveAndDuplicateDigitsCannotSkipOrCreditExerciseTwice() {
        Fixture f=mathWithAnswer(7);f.c.close();
        List<Runnable> saves=new ArrayList<>();f.c=f.controller(saves::add);
        f.c.foreground(true);f.c.resume();saves.remove(0).run();
        f.c.digit(7);f.c.digit(7);f.now=10000;f.c.tick();
        assertEquals(0,f.c.state().correct);assertEquals(1,saves.size());
        saves.remove(0).run();assertEquals(1,f.c.state().correct);
        f.now+=1199;f.c.tick();assertEquals(123,f.c.state().exercise.id);
        f.now++;f.c.tick();f.c.tick();f.c.digit(7);
        assertEquals(1,saves.size());saves.remove(0).run();
        assertNotEquals(123,f.c.state().exercise.id);assertEquals(1,f.c.state().correct);
    }

    @Test public void failedAutomaticSolveStaysPausedUntilDurableRetryAndFreshDelay() {
        Fixture f=mathWithAnswer(7);f.store.fail=true;f.c.digit(7);
        assertNotNull(f.c.error());assertFalse(f.c.state().solved);assertEquals(0,f.c.state().correct);
        f.now=100000;f.c.tick();assertEquals(123,f.c.state().exercise.id);
        f.store.fail=false;f.c.retrySave();assertTrue(f.c.state().paused);
        assertTrue(f.c.state().solved);assertEquals(1,f.c.state().correct);
        f.c.resume();f.now+=1200;f.c.tick();
        assertNotEquals(123,f.c.state().exercise.id);assertEquals(1,f.c.state().correct);
    }

    @Test public void finalAutomaticAnswerShowsSuccessBeforeHandoffWithoutAnotherExercise() {
        Fixture f=mathWithAnswer(7);
        f.c.state().correct=2;f.c.state().elapsedMs=299999;
        f.now=1;f.c.digit(7);
        assertEquals(Session.Phase.MATH,f.c.state().phase);
        assertTrue(f.c.state().solved);assertEquals(3,f.c.state().correct);
        assertEquals("Goed gedaan!",f.c.state().feedback);
        f.now+=1199;f.c.tick();assertEquals(Session.Phase.MATH,f.c.state().phase);
        f.now++;f.c.tick();assertEquals(Session.Phase.BREAK,f.c.state().phase);
        assertEquals(123,f.c.state().exercise.id);assertFalse(f.c.state().paused);
        f.c.tick();f.c.digit(7);assertEquals(3,f.c.state().correct);
    }

    @Test public void timeBoundaryDuringSuccessFinishesCurrentExerciseAfterDelay() {
        Fixture f=mathWithAnswer(7);
        f.c.state().correct=2;f.c.state().elapsedMs=299500;f.c.digit(7);
        f.now=500;f.c.tick();assertEquals(Session.Phase.MATH,f.c.state().phase);
        f.now=1200;f.c.tick();assertEquals(Session.Phase.BREAK,f.c.state().phase);
        assertEquals(123,f.c.state().exercise.id);assertEquals(3,f.c.state().correct);
    }

    @Test public void boundaryCheckpointExcludesSaveLatencyFromVisibleSuccessDelay() {
        Fixture f=mathWithAnswer(7);f.c.close();
        List<Runnable> saves=new ArrayList<>();f.c=f.controller(saves::add);
        f.c.foreground(true);f.c.resume();saves.remove(0).run();
        f.c.state().correct=2;f.c.state().elapsedMs=299500;
        f.c.digit(7);saves.remove(0).run();
        f.now=500;f.c.tick();assertTrue(f.c.busy());
        f.now=10000;f.c.tick();assertEquals(Session.Phase.MATH,f.c.state().phase);
        saves.remove(0).run();
        f.now=10699;f.c.tick();assertFalse(f.c.busy());
        f.now=10700;f.c.tick();assertTrue(f.c.busy());
        saves.remove(0).run();assertEquals(Session.Phase.BREAK,f.c.state().phase);
    }

    @Test public void failedAutomaticAdvanceRetainsSolvedExerciseAndOneCredit() {
        Fixture f=mathWithAnswer(7);f.c.digit(7);f.store.fail=true;
        f.now=1200;f.c.tick();
        assertNotNull(f.c.error());assertTrue(f.c.state().paused);
        assertTrue(f.c.state().solved);assertEquals(123,f.c.state().exercise.id);
        assertEquals(1,f.c.state().correct);
        f.store.fail=false;f.c.retrySave();
        assertNotEquals(123,f.c.state().exercise.id);assertTrue(f.c.state().paused);
        assertEquals(1,f.c.state().correct);
    }
    @Test public void presentationGateWaitsForLandingAndForegroundGap() {
        Fixture f=new Fixture(); List<PresentationEvent> events=new ArrayList<>();
        f.c.setPresenter(events::add); f.c.foreground(true); f.c.resume();
        f.c.chooseSquare(52);f.c.chooseSquare(36);
        assertEquals(1,events.size());assertTrue(f.computer.isEmpty());
        PresentationEvent event=events.get(0);
        assertEquals("e2e4",event.move); assertEquals('P',event.beforePiece(52));
        assertEquals('.',event.beforePiece(36));assertEquals('P',event.afterPiece(36));
        f.c.focusSquare(35);f.c.completePresentation(event.id);f.c.completePresentation(event.id);
        f.now=99;f.c.tick();assertTrue(f.computer.isEmpty());
        f.now=100;f.c.tick();assertEquals(1,f.computer.size());
    }
    @Test public void missingLandingTimesOutAndStaleAckCannotRestartAfterPause() {
        Fixture f=new Fixture(); List<PresentationEvent> events=new ArrayList<>();
        f.c.setPresenter(events::add);f.c.foreground(true);f.c.resume();
        f.c.chooseSquare(52);f.c.chooseSquare(36);
        f.now=2000;f.c.tick();assertNull(f.c.presentation());
        f.c.pause();f.c.completePresentation(events.get(0).id);
        f.now=5000;f.c.tick();assertTrue(f.computer.isEmpty());
        f.c.resume();assertEquals(1,f.computer.size());
    }
    @Test public void failedMoveAndSilentRetryEmitNoPresentation() {
        Fixture f=new Fixture();List<PresentationEvent> events=new ArrayList<>();
        f.c.setPresenter(events::add);f.c.foreground(true);f.c.resume();
        f.c.chooseSquare(52);f.store.fail=true;f.c.chooseSquare(36);
        assertTrue(events.isEmpty());f.store.fail=false;f.c.retrySave();assertTrue(events.isEmpty());
    }
    @Test public void correctMathEmitsOneTypedEventWithoutReplayingResume() {
        Fixture f=mathWithAnswer(7);List<PresentationEvent> events=new ArrayList<>();
        f.c.setPresenter(events::add);f.c.digit(7);f.c.digit(7);
        assertEquals(1,events.size());assertEquals(PresentationEvent.Kind.MATH_SUCCESS,events.get(0).kind);
        f.c.pause();f.c.resume();assertEquals(1,events.size());
    }
    @Test public void presenterDetachesWithoutLosingOwedTurn() {
        Fixture f=new Fixture();f.c.setPresenter(e->{});f.c.foreground(true);f.c.resume();
        f.c.chooseSquare(52);f.c.chooseSquare(36);assertTrue(f.computer.isEmpty());
        f.c.setPresenter(null);assertNull(f.c.presentation());assertEquals(1,f.computer.size());
    }
    @Test public void focusLossDuringMoveSaveDoesNotEmitLateEvent() {
        Fixture f=new Fixture();List<Runnable> saves=new ArrayList<>();List<PresentationEvent> events=new ArrayList<>();
        f.c.close();f.c=f.controller(saves::add);f.c.setPresenter(events::add);
        f.c.foreground(true);f.c.resume();saves.remove(0).run();
        f.c.chooseSquare(52);saves.remove(0).run();f.c.chooseSquare(36);
        f.c.foreground(false);saves.remove(0).run();saves.remove(0).run();
        assertTrue(events.isEmpty());assertEquals(1,f.c.state().moves.size());assertTrue(f.c.state().paused);
    }
    @Test public void synchronousPresenterCompletionStillSchedulesExactlyOneReply() {
        Fixture f=new Fixture();f.c.setPresenter(e->f.c.completePresentation(e.id));
        f.c.foreground(true);f.c.resume();f.c.chooseSquare(52);f.c.chooseSquare(36);
        assertNull(f.c.presentation());assertTrue(f.computer.isEmpty());
        f.now=100;f.c.tick();f.c.tick();assertEquals(1,f.computer.size());
        f.computer.get(0).run();assertEquals(2,f.c.state().moves.size());assertNull(f.c.presentation());
    }
    @Test public void resetDuringGateRejectsStaleCompletionAndReplacesGeneration() {
        Fixture f=new Fixture();List<PresentationEvent> events=new ArrayList<>();
        f.c.setPresenter(events::add);f.c.foreground(true);f.c.resume();
        f.c.chooseSquare(52);f.c.chooseSquare(36);PresentationEvent old=events.get(0);
        f.c.resetSession();f.c.completePresentation(old.id);f.now=3000;f.c.tick();
        assertTrue(f.computer.isEmpty());assertTrue(f.c.state().moves.isEmpty());
        f.c.skipToChess();f.c.resume();f.c.chooseSquare(52);f.c.chooseSquare(36);
        assertTrue(events.get(1).id.generation>old.id.generation);
        f.c.completePresentation(old.id);assertEquals(events.get(1),f.c.presentation());
    }
    @Test public void blackReplyAfterFiveMinutesStillOffersLandingWithoutEndingGame() {
        Fixture f=new Fixture();List<PresentationEvent> events=new ArrayList<>();
        f.c.setPresenter(events::add);f.c.foreground(true);f.c.resume();f.c.chooseSquare(52);
        f.now=300000;f.c.chooseSquare(36);PresentationEvent white=events.get(0);
        f.c.completePresentation(white.id);f.now+=100;f.c.tick();f.computer.get(0).run();
        assertEquals(Session.Phase.CHESS,f.c.state().phase);assertFalse(f.c.state().paused);
        assertEquals(2,events.size());assertEquals(events.get(1),f.c.presentation());
        assertFalse(events.get(1).beforeWhiteToMove);
        assertEquals(1,events.get(0).id.subject);assertEquals(2,events.get(1).id.subject);
        f.c.completePresentation(events.get(1).id);f.now+=1000;f.c.tick();
        assertEquals(1,f.computer.size());assertNull(f.c.presentation());
        assertEquals(Session.Phase.CHESS,f.c.state().phase);
    }
    @Test public void breakCountsOnlyForegroundTimeRestoresPausedAndStartsChessAutomatically() {
        Fixture f=new Fixture();Session saved=new Session();saved.phase=Session.Phase.BREAK;saved.elapsedMs=10000;
        f.store.saved=saved;f.c.close();f.c=f.controller(Runnable::run);
        assertTrue(f.c.state().paused);assertEquals(50000,f.c.breakRemainingMs());
        f.c.foreground(true);f.c.resume();f.now=10000;assertEquals(40000,f.c.breakRemainingMs());
        f.c.foreground(false);assertEquals(20000,f.c.state().elapsedMs);
        f.now=1000000;f.c.tick();assertEquals(40000,f.c.breakRemainingMs());
        f.c.close();f.c=f.controller(Runnable::run);assertTrue(f.c.state().paused);assertEquals(40000,f.c.breakRemainingMs());
        f.c.foreground(true);f.c.resume();f.now+=39999;f.c.tick();
        assertEquals(Session.Phase.BREAK,f.c.state().phase);assertEquals(1,f.c.breakRemainingMs());
        f.now++;f.c.tick();assertEquals(Session.Phase.CHESS,f.c.state().phase);
        assertFalse(f.c.state().paused);assertEquals(0,f.c.state().elapsedMs);
    }
    @Test public void completedGameWaitsForExplicitContinueThenNextMathBreakStartsFreshBoard() {
        Fixture f=new Fixture();Session ended=new Session();ended.phase=Session.Phase.CHESS;
        ended.moves.addAll(java.util.Arrays.asList("f2f3","e7e5","g2g4","d8h4"));
        ended.result=ChessGame.fromHistory(ended.moves).result();assertFalse(ended.result.isEmpty());
        f.store.saved=ended;f.c.close();f.c=f.controller(Runnable::run);f.c.foreground(true);
        f.now=900000;f.c.tick();assertEquals(Session.Phase.CHESS,f.c.state().phase);
        f.c.resume();assertEquals(Session.Phase.MATH,f.c.state().phase);assertEquals(4,f.c.state().moves.size());
        f.c.state().correct=2;f.c.state().elapsedMs=300000;answer(f.c);f.now+=1200;f.c.tick();
        assertEquals(Session.Phase.BREAK,f.c.state().phase);assertEquals(4,f.c.state().moves.size());
        f.now+=60000;f.c.tick();assertEquals(Session.Phase.CHESS,f.c.state().phase);
        assertTrue(f.c.state().moves.isEmpty());assertEquals("",f.c.state().result);assertFalse(f.c.state().paused);
    }
    @Test public void failedBreakCompletionRetainsBreakUntilPausedDurableRetry() {
        Fixture f=new Fixture();Session saved=new Session();saved.phase=Session.Phase.BREAK;saved.elapsedMs=59999;
        f.store.saved=saved;f.c.close();f.c=f.controller(Runnable::run);f.c.foreground(true);f.c.resume();
        f.store.fail=true;f.now=1;f.c.tick();assertEquals(Session.Phase.BREAK,f.c.state().phase);
        assertTrue(f.c.state().paused);assertNotNull(f.c.error());
        f.store.fail=false;f.c.retrySave();assertEquals(Session.Phase.CHESS,f.c.state().phase);assertTrue(f.c.state().paused);
    }
}
