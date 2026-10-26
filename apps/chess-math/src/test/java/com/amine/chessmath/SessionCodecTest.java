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
        String raw=SessionCodec.encode(new Session()).replace("\"schema\":2", "\"schema\":99");
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
}
