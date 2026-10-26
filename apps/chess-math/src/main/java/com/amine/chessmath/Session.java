package com.amine.chessmath;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One locally saved game and practice round. The controller owns mutations. */
public final class Session {
    public enum Phase { CHESS, TO_MATH, MATH, TO_CHESS }
    public String id = UUID.randomUUID().toString();
    public long revision, elapsedMs;
    public Phase phase = Phase.CHESS;
    public boolean paused = true, chessPending, mathPending, solved;
    public int correct, selected = -1, focus = 52, promotionTo = -1, hintLevel;
    public String intendedMove, input = "", feedback = "", result = "";
    public List<String> moves = new ArrayList<>();
    public MathConfig config = new MathConfig();
    public MathConfig exerciseConfig;
    public Exercise exercise;
    public long opponentSeed = System.nanoTime();
    public String generatorState = new MathGenerator(opponentSeed ^ 0x51a7L).snapshot();

    public Session copy() {
        Session s = new Session();
        s.id=id; s.revision=revision; s.elapsedMs=elapsedMs; s.phase=phase;
        s.paused=paused; s.chessPending=chessPending; s.mathPending=mathPending; s.solved=solved;
        s.correct=correct; s.selected=selected; s.focus=focus; s.promotionTo=promotionTo;
        s.hintLevel=hintLevel; s.intendedMove=intendedMove; s.input=input;
        s.feedback=feedback; s.result=result; s.moves=new ArrayList<>(moves);
        s.config=config.copy(); s.exerciseConfig=exerciseConfig==null?null:exerciseConfig.copy();
        s.exercise=exercise; s.opponentSeed=opponentSeed; s.generatorState=generatorState;
        return s;
    }
}
