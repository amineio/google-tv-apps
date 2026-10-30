package com.amine.chessmath;

import org.junit.Test;
import static org.junit.Assert.*;

public class SessionCodecTest {
    @Test public void restoresMoveHistoryAndExerciseWithoutCredit() throws Exception {
        Session s=new Session(); s.moves.add("e2e4"); s.moves.add("e7e5");
        s.phase=Session.Phase.MATH; s.elapsedMs=299000; s.correct=2;
        MathGenerator generator=MathGenerator.restore(s.generatorState);
        s.exercise=generator.next(s.config); s.exerciseConfig=s.config.copy();
        s.generatorState=generator.snapshot(); s.input="12"; s.hintLevel=1;
        Session copy=SessionCodec.decode(SessionCodec.encode(s));
        assertEquals(s.moves,copy.moves); assertEquals(s.generatorState,copy.generatorState);
        assertEquals(2,copy.correct); assertFalse(copy.solved);
        assertEquals(s.exercise.answer(),copy.exercise.answer());
        assertEquals(s.exercise.id,copy.exercise.id);
        assertEquals("12",copy.input);
    }
    @Test public void rejectsIllegalRestoredMoveAndUnknownSchema() throws Exception {
        Session s=new Session(); s.moves.add("e2e5");
        try { SessionCodec.decode(SessionCodec.encode(s)); fail(); } catch(IllegalArgumentException expected) { }
        String raw=SessionCodec.encode(new Session()).replace("\"schema\":3", "\"schema\":99");
        try { SessionCodec.decode(raw); fail(); } catch(IllegalArgumentException expected) { }
    }
    @Test public void rejectsWrongAnswerCreditedAsSolved() throws Exception {
        Session s=new Session(); s.phase=Session.Phase.MATH;
        s.exercise=new Exercise(1,2,3,"ADD"); s.exerciseConfig=s.config.copy();
        s.solved=true; s.correct=1; s.input="6";
        try { SessionCodec.decode(SessionCodec.encode(s)); fail(); } catch(IllegalArgumentException expected) { }
    }
    @Test public void breakRoundTripsWithRemainingTimeAndPriorBoard() throws Exception {
        Session s=new Session();s.phase=Session.Phase.BREAK;s.elapsedMs=12345;
        s.moves.add("e2e4");s.moves.add("e7e5");
        Session copy=SessionCodec.decode(SessionCodec.encode(s));
        assertEquals(Session.Phase.BREAK,copy.phase);assertEquals(12345,copy.elapsedMs);assertEquals(s.moves,copy.moves);
    }
    @Test public void legacyChessAndHandoffsMigrateWithoutLosingBoardOrExercise() throws Exception {
        Session s=new Session();s.phase=Session.Phase.CHESS;s.moves.add("e2e4");s.moves.add("e7e5");s.elapsedMs=300000;s.chessPending=true;
        org.json.JSONObject json=new org.json.JSONObject(SessionCodec.encode(s));json.put("schema",1);
        org.json.JSONObject noExercise=new org.json.JSONObject(json.toString());noExercise.remove("exercise");noExercise.remove("exerciseConfig");
        Session chess=SessionCodec.decode(noExercise.toString());assertEquals(Session.Phase.CHESS,chess.phase);assertNull(chess.exercise);
        assertEquals(s.moves,chess.moves);assertFalse(chess.chessPending);
        json.put("phase","TO_MATH");Session oldChessHandoff=SessionCodec.decode(json.toString());
        assertEquals(Session.Phase.CHESS,oldChessHandoff.phase);assertEquals(s.moves,oldChessHandoff.moves);
        json.put("phase","TO_CHESS").put("correct",3).put("solved",true).put("input",Integer.toString(s.exercise.answer()));
        Session oldMathHandoff=SessionCodec.decode(json.toString());assertEquals(Session.Phase.BREAK,oldMathHandoff.phase);
        assertEquals(0,oldMathHandoff.elapsedMs);assertEquals(s.moves,oldMathHandoff.moves);
        assertEquals(s.exercise.id,oldMathHandoff.exercise.id);assertTrue(oldMathHandoff.solved);assertTrue(oldMathHandoff.paused);
    }
    @Test public void thinkingTimeRoundTripsAndOldSavesDefaultToZero() throws Exception {
        Session s=new Session();s.moves.add("e2e4");s.moves.add("e7e5");
        org.json.JSONObject json=new org.json.JSONObject(SessionCodec.encode(s));
        json.put("thinkingRemainingMs",12345).put("thinkingTotalMs",20000);
        org.json.JSONObject copy=new org.json.JSONObject(SessionCodec.encode(SessionCodec.decode(json.toString())));
        assertEquals(12345,copy.getLong("thinkingRemainingMs"));assertEquals(20000,copy.getLong("thinkingTotalMs"));
        json.remove("thinkingRemainingMs");json.remove("thinkingTotalMs");
        copy=new org.json.JSONObject(SessionCodec.encode(SessionCodec.decode(json.toString())));
        assertEquals(0,copy.getLong("thinkingRemainingMs"));assertEquals(0,copy.getLong("thinkingTotalMs"));
    }
    @Test public void rejectsInvalidThinkingTimeAndWrongBoardState() throws Exception {
        Session s=new Session();s.moves.add("e2e4");s.moves.add("e7e5");
        long[][] invalid={{-1,20000},{60001,60001},{20001,20000},{1,0},{0,60001}};
        for(long[] value:invalid){
            org.json.JSONObject json=new org.json.JSONObject(SessionCodec.encode(s));
            json.put("thinkingRemainingMs",value[0]).put("thinkingTotalMs",value[1]);
            try{SessionCodec.decode(json.toString());fail("Invalid thinking duration accepted");}catch(IllegalArgumentException expected){}
        }
        for(String[] moves:new String[][]{ {}, {"e2e4"}, {"f2f3","e7e5","g2g4","d8h4"} }){
            s.moves=new java.util.ArrayList<>(java.util.Arrays.asList(moves));
            s.result=ChessGame.fromHistory(s.moves).result();
            org.json.JSONObject json=new org.json.JSONObject(SessionCodec.encode(s));
            json.put("thinkingRemainingMs",1000).put("thinkingTotalMs",20000);
            try{SessionCodec.decode(json.toString());fail("Wait on invalid board accepted");}catch(IllegalArgumentException expected){}
        }
    }


    @Test public void schemaThreeRoundTripsLocalModeProfilesAndCopy() throws Exception {
        LocalChessTest.Fixture f=new LocalChessTest.Fixture();f.local();f.move("e2e4");
        Session copy=SessionCodec.decode(SessionCodec.encode(f.saved)).copy();
        assertEquals(3,new org.json.JSONObject(SessionCodec.encode(copy)).getInt("schema"));
        assertEquals("LOCAL",LocalChessTest.mode(copy));assertEquals("Amber",LocalChessTest.name(copy,"whiteProfile"));
        assertEquals(f.saved.moves,copy.moves);
    }
    @Test public void legacySchemasRemainComputerAndPreserveProgress() throws Exception {
        for(int schema:new int[]{1,2}) {Session s=new Session();s.correct=2;s.input="12";s.moves.add("e2e4");
            org.json.JSONObject json=new org.json.JSONObject(SessionCodec.encode(s));json.put("schema",schema);json.remove("mode");json.remove("whiteProfile");json.remove("blackProfile");
            Session copy=SessionCodec.decode(json.toString());assertEquals("COMPUTER",LocalChessTest.mode(copy));assertEquals(2,copy.correct);assertEquals(s.moves,copy.moves);assertEquals(s.exercise.id,copy.exercise.id);
        }
    }
    @Test public void restoresBlackSelectionAndPromotionForEveryPiece() throws Exception {
        for(char piece:new char[]{'q','r','b','n'}) {LocalChessTest.Fixture f=new LocalChessTest.Fixture();f.local();
            for(String m:new String[]{"a2a4","h7h5","a4a5","h5h4","a5a6","h4h3","a6b7","h3g2","b7a8n"})f.move(m);
            f.c.chooseSquare(LocalChessTest.square("g2"));f.c.chooseSquare(LocalChessTest.square("h1"));
            Session restored=SessionCodec.decode(SessionCodec.encode(f.saved));assertEquals(LocalChessTest.square("g2"),restored.selected);assertEquals(LocalChessTest.square("h1"),restored.promotionTo);
            f.c.close();f.saved=restored;SessionController c=new SessionController(new SessionController.Persistence(){public Session load(){return f.saved.copy();}public void save(Session next){f.saved=next.copy();}},()->0L,Runnable::run,Runnable::run,f.ai::add,()->{});
            c.foreground(true);c.resume();c.promote(piece);assertEquals("g2h1"+piece,f.saved.moves.get(9));assertTrue(f.ai.isEmpty());c.close();
        }
    }
    @Test public void rejectsInvalidModeAndLocalMathOrWait() throws Exception {
        LocalChessTest.Fixture f=new LocalChessTest.Fixture();f.local();
        for(String field:new String[]{"mode","phase","thinkingTotalMs"}) {org.json.JSONObject json=new org.json.JSONObject(SessionCodec.encode(f.saved));json.put(field,field.equals("mode")?"REMOTE":field.equals("phase")?"MATH":1);
            try{SessionCodec.decode(json.toString());fail("Invalid local snapshot accepted: "+field);}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void unknownAndDuplicatePortraitsRecoverToDistinctDefaults() throws Exception {
        LocalChessTest.Fixture f=new LocalChessTest.Fixture();f.local();org.json.JSONObject json=new org.json.JSONObject(SessionCodec.encode(f.saved));
        json.getJSONObject("whiteProfile").put("portraitId","missing");json.getJSONObject("blackProfile").put("portraitId","missing");
        Session copy=SessionCodec.decode(json.toString());Object white=copy.getClass().getField("whiteProfile").get(copy),black=copy.getClass().getField("blackProfile").get(copy);
        assertNotEquals(white.getClass().getField("portraitId").get(white),black.getClass().getField("portraitId").get(black));assertEquals("Amber",LocalChessTest.name(copy,"whiteProfile"));
    }
    @Test public void malformedRestoredNamesFallBackWithoutDiscardingLocalBoard() throws Exception {
        LocalChessTest.Fixture f=new LocalChessTest.Fixture();f.local();f.move("e2e4");f.move("e7e5");
        for(String invalid:new String[]{"A\nB",String.join("",java.util.Collections.nCopies(25,"x"))}){
            org.json.JSONObject json=new org.json.JSONObject(SessionCodec.encode(f.saved));
            json.getJSONObject("whiteProfile").put("customName",invalid);
            json.getJSONObject("blackProfile").put("customName",invalid);
            Session restored=SessionCodec.decode(json.toString());
            assertEquals(Session.Mode.LOCAL,restored.mode);assertEquals(f.saved.moves,restored.moves);
            assertNull(restored.whiteProfile.customName);assertNull(restored.blackProfile.customName);
            assertEquals("fox",restored.whiteProfile.portraitId);assertEquals("owl",restored.blackProfile.portraitId);
        }
    }
}
