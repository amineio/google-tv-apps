package com.amine.chessmath;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Drives the real controller with deterministic persistence and queued computer work. */
public class LocalChessTest {
    static final class Fixture {
        Session saved=new Session(); final List<Runnable> ai=new ArrayList<>(); long now;
        final SessionController c=new SessionController(new SessionController.Persistence(){
            public Session load(){return saved.copy();}
            public void save(Session s){saved=s.copy();}
        },()->now,Runnable::run,Runnable::run,ai::add,()->{});
        Fixture(){c.foreground(true);}
        void local(){start(c);}
        void move(String uci){c.chooseSquare(square(uci.substring(0,2)));c.chooseSquare(square(uci.substring(2,4)));if(uci.length()==5)c.promote(uci.charAt(4));}
    }
    static int square(String s){return (8-(s.charAt(1)-'0'))*8+s.charAt(0)-'a';}
    static Object profile(String id,String name){try{return Class.forName("com.amine.chessmath.PlayerProfile").getConstructor(String.class,String.class).newInstance(id,name);}catch(Exception e){throw new AssertionError("PlayerProfile contract missing",e);}}
    static void start(SessionController c){try{Class<?> type=Class.forName("com.amine.chessmath.PlayerProfile");c.getClass().getMethod("startLocalGame",type,type).invoke(c,profile("fox","Amber"),profile("owl",null));}catch(Exception e){throw new AssertionError("Local start contract missing",e);}}
    static String mode(Session s){try{return s.getClass().getField("mode").get(s).toString();}catch(Exception e){throw new AssertionError("Mode contract missing",e);}}
    @Test public void alternatesWhiteAndBlackWithoutComputerOrWait(){Fixture f=new Fixture();f.local();f.move("e2e4");f.move("e7e5");assertEquals(Arrays.asList("e2e4","e7e5"),f.saved.moves);assertTrue(f.ai.isEmpty());assertEquals(0,f.c.thinkingRemainingMs());assertEquals(0,f.c.thinkingTotalMs());}
    @Test public void rejectsWrongColorSelectionOnBothTurns(){Fixture f=new Fixture();f.local();f.c.chooseSquare(square("e7"));assertEquals(-1,f.saved.selected);f.move("e2e4");f.c.chooseSquare(square("d2"));assertEquals(-1,f.saved.selected);f.c.chooseSquare(square("e7"));assertEquals(square("e7"),f.saved.selected);}
    @Test public void localNeverEntersArithmeticAfterLongTimeOrReset(){Fixture f=new Fixture();f.local();f.now=900000;f.c.tick();f.c.skipToMath();f.c.resume();assertEquals(Session.Phase.CHESS,f.saved.phase);f.c.resetSession();assertEquals("LOCAL",mode(f.saved));assertEquals(Session.Phase.CHESS,f.saved.phase);}
    @Test public void staleComputerCallbackCannotModifyReplacement(){Fixture f=new Fixture();f.c.setMathsEnabled(false);f.c.resume();f.move("e2e4");assertEquals(1,f.ai.size());Runnable stale=f.ai.remove(0);f.local();stale.run();assertEquals("LOCAL",mode(f.saved));assertTrue(f.saved.moves.isEmpty());}
    @Test public void rematchRetainsParticipantsAndMode(){Fixture f=new Fixture();f.local();for(String m:new String[]{"f2f3","e7e5","g2g4","d8h4"})f.move(m);assertFalse(f.saved.result.isEmpty());List<String> finalBoard=new ArrayList<>(f.saved.moves);f.now=900000;f.c.tick();assertEquals(finalBoard,f.saved.moves);f.c.continueAfterGame();assertEquals("LOCAL",mode(f.saved));assertTrue(f.saved.moves.isEmpty());assertEquals(Session.Phase.CHESS,f.saved.phase);assertEquals("Amber",name(f.saved,"whiteProfile"));}
    static String name(Session s,String field){try{return (String)s.getClass().getField(field).get(s).getClass().getField("customName").get(s.getClass().getField(field).get(s));}catch(Exception e){throw new AssertionError(e);}}
    @Test public void whiteAndBlackCanChooseEveryPromotion(){for(char piece:new char[]{'q','r','b','n'})for(boolean black:new boolean[]{false,true}){Fixture f=new Fixture();f.local();for(String m:new String[]{"a2a4","h7h5","a4a5","h5h4","a5a6","h4h3","a6b7","h3g2"})f.move(m);if(black){f.move("b7a8n");f.move("g2h1"+piece);assertEquals("g2h1"+piece,f.saved.moves.get(9));}else{f.move("b7a8"+piece);assertEquals("b7a8"+piece,f.saved.moves.get(8));}}}
    @Test public void bothColorsCanCastle(){Fixture f=new Fixture();f.local();for(String m:new String[]{"e2e4","e7e5","g1f3","g8f6","f1e2","f8e7","e1g1","e8g8"})f.move(m);assertEquals(8,f.saved.moves.size());assertEquals('K',f.c.game().pieceAt(square("g1")));assertEquals('k',f.c.game().pieceAt(square("g8")));}
    @Test public void blackCanCaptureEnPassant(){Fixture f=new Fixture();f.local();for(String m:new String[]{"a2a3","e7e5","a3a4","e5e4","d2d4","e4d3"})f.move(m);assertEquals(6,f.saved.moves.size());assertEquals('.',f.c.game().pieceAt(square("d4")));}
    @Test public void blackCanClaimDrawWithIntendedMove(){Fixture f=new Fixture();f.local();for(String m:new String[]{"g1f3","g8f6","f3g1","f6g8","g1f3","g8f6","f3g1"})f.move(m);f.move("f6g8");assertEquals("f6g8",f.saved.intendedMove);f.c.claimDraw();assertEquals("Remise geclaimd",f.saved.result);}
    @Test public void blackCanPlayIntendedMoveInsteadOfClaiming(){Fixture f=new Fixture();f.local();for(String m:new String[]{"g1f3","g8f6","f3g1","f6g8","g1f3","g8f6","f3g1"})f.move(m);f.move("f6g8");f.c.playIntendedMove();assertEquals(8,f.saved.moves.size());assertEquals("",f.saved.result);}
    @Test public void failedLocalStartKeepsExistingBoard(){Fixture f=new Fixture();f.c.setMathsEnabled(false);f.c.resume();f.move("e2e4");Object white=profile("fox",null),black=profile("fox",null);try{Class<?> t=white.getClass();f.c.getClass().getMethod("startLocalGame",t,t).invoke(f.c,white,black);fail();}catch(java.lang.reflect.InvocationTargetException expected){assertTrue(expected.getCause() instanceof IllegalArgumentException);}catch(Exception e){throw new AssertionError(e);}assertEquals(Arrays.asList("e2e4"),f.saved.moves);}
    @Test public void failedLocalStartSaveKeepsExistingMatchAndProfilesUntilRetry(){
        final Session[] saved={new Session()};saved[0].phase=Session.Phase.CHESS;saved[0].moves.add("e2e4");
        final boolean[] fail={true};
        SessionController c=new SessionController(new SessionController.Persistence(){
            public Session load(){return saved[0].copy();}
            public void save(Session next) throws Exception{if(fail[0])throw new java.io.IOException("disk full");saved[0]=next.copy();}
        },()->0L,Runnable::run,Runnable::run,r->{},()->{});
        c.foreground(true);start(c);assertNotNull(c.error());assertEquals("COMPUTER",mode(c.state()));
        assertEquals(Arrays.asList("e2e4"),c.state().moves);assertEquals(Arrays.asList("e2e4"),saved[0].moves);
        fail[0]=false;c.retrySave();assertEquals("LOCAL",mode(saved[0]));assertTrue(saved[0].paused);assertEquals("Amber",name(saved[0],"whiteProfile"));assertTrue(saved[0].moves.isEmpty());
    }
}
