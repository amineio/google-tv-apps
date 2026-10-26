package com.amine.chessmath;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** All public actions and executor callbacks belong to one UI event queue. */
public final class SessionController implements AutoCloseable {
    public interface Persistence {
        Session load() throws Exception;
        void save(Session session) throws Exception;
    }
    private final Persistence storage;
    private final LongSupplier clock;
    private final Executor main, io, computer;
    private Runnable listener;
    private final boolean ownExecutors;
    private Session state, failedSave;
    private ChessGame game;
    private boolean foreground, busy, pendingPause, corrupt, closed, workerRunning;
    private long anchor=-1, epoch, saveStarted;
    private int latestFocus;
    private String error;

    public SessionController(Persistence storage, LongSupplier clock, Executor main, Runnable changed) {
        this(storage,clock,main,Executors.newSingleThreadExecutor(),Executors.newSingleThreadExecutor(),changed,true);
    }
    SessionController(Persistence storage, LongSupplier clock, Executor main, Executor io,
                      Executor computer, Runnable changed) {
        this(storage,clock,main,io,computer,changed,false);
    }
    private SessionController(Persistence storage, LongSupplier clock, Executor main, Executor io,
                              Executor computer, Runnable changed, boolean own) {
        this.storage=storage; this.clock=clock; this.main=main; this.io=io;
        this.computer=computer; this.listener=changed; this.ownExecutors=own;
        try { state=storage.load(); if(state==null)state=new Session(); }
        catch(Exception invalid) {
            state=new Session(); corrupt=true;
            error="Het opgeslagen spel kan niet worden geopend. Een ouder kan opnieuw beginnen.";
        }
        state.paused=true;
        try { game=ChessGame.fromHistory(state.moves); }
        catch(RuntimeException invalid) { game=new ChessGame(); corrupt=true; error="Het opgeslagen spel is ongeldig. Vraag een ouder om hulp."; }
        latestFocus=state.focus;
    }

    public Session state() { return state; }
    public ChessGame game() { return game; }
    public boolean busy() { return busy; }
    public String error() { return error; }
    public boolean corruptSave() { return corrupt; }
    public void setListener(Runnable listener) { this.listener=listener; }
    private void changed() { if(listener!=null)listener.run(); }

    public void foreground(boolean visible) {
        if(closed)return;
        if(!visible)pause();
        foreground=visible;
    }
    public void resume() {
        if(!foreground||busy||error!=null||closed)return;
        action(s->{
            if(s.phase==Session.Phase.TO_MATH) beginMath(s);
            else if(s.phase==Session.Phase.TO_CHESS) {
                s.phase=Session.Phase.CHESS; s.elapsedMs=0; s.chessPending=false;
                s.mathPending=false; s.correct=0;
                if(!s.result.isEmpty()) clearBoard(s);
            }
            s.paused=false;
        },true);
    }
    public void pause() {
        if(closed)return;
        epoch++; workerRunning=false;
        if(busy) { pendingPause=true; anchor=-1; return; }
        if(state.paused) { anchor=-1; return; }
        Session next=settled(); next.paused=true; commit(next);
    }

    public void tick() {
        if(closed)return;
        if(busy) {
            if(clock.getAsLong()-saveStarted>2000 && error==null) {
                error="Opslaan duurt langer. De tijd staat stil."; changed();
            }
            return;
        }
        if(!active())return;
        Session next=settled();
        boundary(next);
        if(next.phase!=state.phase || next.chessPending!=state.chessPending
                || next.mathPending!=state.mathPending || next.elapsedMs-state.elapsedMs>=5000) commit(next);
    }

    public void focusSquare(int square) {
        if(square>=0 && square<64 && !closed) { latestFocus=square;state.focus=square; }
    }
    public void chooseSquare(int square) {
        if(!canPlay()||square<0||square>=64)return;
        action(s->{
            s.focus=square;
            if(s.selected==square) { s.selected=-1; s.feedback=""; }
            else if(Character.isUpperCase(game.pieceAt(square))) { s.selected=square; s.feedback=""; }
            else if(s.selected>=0) {
                String prefix=SessionCodec.uciSquare(s.selected)+SessionCodec.uciSquare(square);
                List<String> matches=new ArrayList<>();
                for(String m:game.legalMoves())if(m.startsWith(prefix))matches.add(m);
                if(matches.size()>1) s.promotionTo=square;
                else if(matches.size()==1) prepareMove(s,matches.get(0));
                else s.feedback="Deze zet kan niet.";
            }
        },false);
    }
    public void cancelSelection() {
        if(busy||closed||error!=null)return;
        action(s->{
            if(s.intendedMove!=null)s.intendedMove=null;
            else if(s.promotionTo>=0)s.promotionTo=-1;
            else s.selected=-1;
            s.feedback="";
        },false);
    }
    public void promote(char piece) {
        if(!canPlay()||state.promotionTo<0||"qrbn".indexOf(piece)<0)return;
        String move=SessionCodec.uciSquare(state.selected)+SessionCodec.uciSquare(state.promotionTo)+piece;
        if(game.legalMoves().contains(move)) action(s->prepareMove(s,move),false);
    }
    public void playIntendedMove() {
        if(!canAct()||state.intendedMove==null)return;
        String move=state.intendedMove;
        if(game.legalMoves().contains(move))action(s->applyMove(s,move),false);
    }
    public void claimDraw() {
        if(!canAct()||state.phase!=Session.Phase.CHESS||!state.result.isEmpty())return;
        boolean valid=state.intendedMove==null?game.claimableDraw():game.claimableAfter(state.intendedMove);
        if(valid)action(s->{s.result="Remise geclaimd"; s.intendedMove=null; s.selected=-1; s.promotionTo=-1;},true);
    }

    public void digit(int digit) {
        if(!canMath()||state.solved)return;
        action(s->{s.input=MathInput.append(s.input,digit); s.feedback="";},false);
    }
    public void deleteDigit() {
        if(!canMath()||state.solved)return;
        action(s->{s.input=MathInput.delete(s.input);s.feedback="";},false);
    }
    public void submitAnswer() {
        if(!canMath()||state.solved)return;
        action(s->{
            if(s.input.isEmpty())s.feedback="Vul je antwoord in.";
            else if(MathInput.matches(s.input,s.exercise)) {
                s.solved=true; s.correct++; s.feedback="Goed gedaan!";
            } else { s.feedback="Probeer nog eens."; s.hintLevel=Math.max(s.hintLevel,1); }
        },false);
    }
    public void nextExercise() {
        if(!canMath()||!state.solved)return;
        action(this::generateExercise,false);
    }
    public void hint() {
        if(!canMath()||state.solved)return;
        action(s->s.hintLevel=Math.min(2,s.hintLevel+1),false);
    }
    public void saveConfig(MathConfig config) {
        config.validate();
        if(!canAct())return;
        action(s->s.config=config.copy(),true);
    }
    public void skipToMath() {
        if(!canAct())return;
        epoch++; workerRunning=false;
        action(s->{beginMath(s);s.paused=true;},true);
    }
    public void skipToChess() {
        if(!canAct())return;
        epoch++; workerRunning=false;
        action(s->{s.phase=Session.Phase.CHESS;s.elapsedMs=0;s.chessPending=false;
            s.mathPending=false;s.paused=true;},true);
    }
    public void restartGame() {
        if(!canAct())return;
        epoch++;workerRunning=false;
        action(this::clearBoard,true);
    }
    public void resetSession() {
        if(busy||closed)return;
        epoch++; workerRunning=false;
        Session fresh=new Session(); fresh.config=state.config.copy();
        fresh.revision=state.revision; corrupt=false;error=null;failedSave=null;
        commit(fresh);
    }
    public void retrySave() {
        if(busy||closed||corrupt)return;
        if(failedSave==null) {
            error=null;state.paused=true;changed();return;
        }
        Session retry=failedSave.copy();retry.paused=true;error=null;failedSave=null;commit(retry);
    }

    private boolean canAct() { return !busy && !closed && error==null && !corrupt; }
    private boolean canPlay() {
        return canAct()&&!state.paused&&foreground&&state.phase==Session.Phase.CHESS
                && state.result.isEmpty()&&game.whiteToMove()&&state.intendedMove==null;
    }
    private boolean canMath() {
        return canAct()&&!state.paused&&foreground&&state.phase==Session.Phase.MATH&&state.exercise!=null;
    }
    private boolean active() {
        return foreground&&!state.paused&&error==null&&!busy
                && ((state.phase==Session.Phase.CHESS && state.result.isEmpty()
                && state.intendedMove==null)||state.phase==Session.Phase.MATH);
    }
    private Session settled() {
        Session next=state.copy();
        if(active()&&anchor>=0)next.elapsedMs+=Math.max(0,clock.getAsLong()-anchor);
        return next;
    }
    private void action(Consumer<Session> mutation,boolean whilePaused) {
        if(!canAct()||(!whilePaused&&state.paused))return;
        Session next=settled();
        if(!whilePaused) {
            boundary(next);
            if(next.phase!=state.phase) {commit(next);return;}
        }
        mutation.accept(next);boundary(next);commit(next);
    }
    private void boundary(Session s) {
        if(s.phase==Session.Phase.CHESS) {
            s.chessPending=s.elapsedMs>=300000;
            ChessGame board=s.moves.equals(state.moves)?game:ChessGame.fromHistory(s.moves);
            if(PhaseRules.chessComplete(s.elapsedMs,board.whiteToMove(),s.selected,
                    s.promotionTo>=0||s.intendedMove!=null,!s.result.isEmpty())) {
                s.phase=Session.Phase.TO_MATH;s.paused=true;
            }
        } else if(s.phase==Session.Phase.MATH) {
            s.mathPending=s.elapsedMs>=300000;
            if(PhaseRules.mathComplete(s.elapsedMs,s.correct,s.solved)) {
                s.phase=Session.Phase.TO_CHESS;s.paused=true;
            }
        }
    }
    private void prepareMove(Session s,String move) {
        if(game.claimableAfter(move)) { s.intendedMove=move;s.promotionTo=-1; }
        else applyMove(s,move);
    }
    private void applyMove(Session s,String move) {
        ChessGame board=ChessGame.fromHistory(s.moves); board.play(move);
        s.moves=board.history();s.result=board.result();s.selected=-1;s.promotionTo=-1;
        s.intendedMove=null;s.feedback="";
        s.opponentSeed=Long.rotateLeft(s.opponentSeed ^ 0x9e3779b97f4a7c15L,17);
    }
    private void beginMath(Session s) {
        s.phase=Session.Phase.MATH;s.elapsedMs=0;s.correct=0;s.mathPending=false;
        generateExercise(s);
    }
    private void generateExercise(Session s) {
        MathGenerator generator=MathGenerator.restore(s.generatorState);
        s.exercise=generator.next(s.config);s.exerciseConfig=s.config.copy();
        s.generatorState=generator.snapshot();s.input="";s.hintLevel=0;s.solved=false;s.feedback="";
    }
    private void clearBoard(Session s) {
        s.moves=new ArrayList<>();s.result="";s.selected=-1;s.focus=52;s.promotionTo=-1;
        s.intendedMove=null;s.feedback="";
    }

    private void commit(Session next) {
        if(next.focus!=state.focus)latestFocus=next.focus;
        next.revision=state.revision+1;
        busy=true;anchor=-1;saveStarted=clock.getAsLong();
        io.execute(()->{
            try {
                storage.save(next);
                main.execute(()->{
                    if(closed)return;
                    boolean boardChanged=!state.moves.equals(next.moves);
                    state=next;
                    state.focus=latestFocus;
                    if(boardChanged)game=ChessGame.fromHistory(state.moves);
                    busy=false;error=null;failedSave=null;
                    if(pendingPause) {
                        pendingPause=false;Session paused=state.copy();paused.paused=true;commit(paused);return;
                    }
                    anchor=active()?clock.getAsLong():-1;
                    changed();scheduleComputer();
                });
            } catch(Exception failure) {
                main.execute(()->{
                    if(closed)return;
                    busy=false;pendingPause=false;state.paused=true;anchor=-1;
                    failedSave=next.copy();failedSave.paused=true;
                    error="Opslaan is niet gelukt. De voortgang blijft staan. Probeer opnieuw.";
                    epoch++;workerRunning=false;changed();
                });
            }
        });
    }
    private void scheduleComputer() {
        if(!canAct()||state.paused||!foreground||state.phase!=Session.Phase.CHESS
                ||!state.result.isEmpty()||game.whiteToMove()||workerRunning)return;
        long token=epoch,revision=state.revision,seed=state.opponentSeed;
        List<String> history=new ArrayList<>(state.moves);
        workerRunning=true;
        computer.execute(()->{
            String move;
            try {
                ChessGame board=ChessGame.fromHistory(history);
                if(board.claimableDraw())move="CLAIM";
                else {
                    move=EasyOpponent.choose(board,seed);
                    if(move!=null&&board.claimableAfter(move))move="CLAIM";
                }
            } catch(RuntimeException failure) { move=null; }
            final String chosen=move;
            main.execute(()->{
                if(closed||token!=epoch)return;
                workerRunning=false;
                if(!canAct()||state.paused||!foreground||state.phase!=Session.Phase.CHESS
                        ||revision!=state.revision||game.whiteToMove()) { scheduleComputer();return; }
                if("CLAIM".equals(chosen)) action(s->{s.result="Remise geclaimd";s.selected=-1;},false);
                else if(chosen!=null&&game.legalMoves().contains(chosen))action(s->applyMove(s,chosen),false);
                else { error="De computer kon geen zet kiezen. Pauzeer en probeer opnieuw.";state.paused=true;changed(); }
            });
        });
    }
    @Override public void close() {
        closed=true;epoch++;anchor=-1;
        if(ownExecutors) {
            ((ExecutorService)io).shutdown();((ExecutorService)computer).shutdownNow();
        }
    }
}
