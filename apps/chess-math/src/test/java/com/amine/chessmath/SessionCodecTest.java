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
        String raw=SessionCodec.encode(new Session()).replace("\"schema\":1", "\"schema\":99");
        try { SessionCodec.decode(raw); fail(); } catch(IllegalArgumentException expected) { }
    }
    @Test public void rejectsWrongAnswerCreditedAsSolved() throws Exception {
        Session s=new Session(); s.phase=Session.Phase.MATH;
        s.exercise=new Exercise(1,2,3,"ADD"); s.exerciseConfig=s.config.copy();
        s.solved=true; s.correct=1; s.input="6";
        try { SessionCodec.decode(SessionCodec.encode(s)); fail(); } catch(IllegalArgumentException expected) { }
    }
}
