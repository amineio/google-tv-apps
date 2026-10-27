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
    public static final long SUCCESS_DELAY_MS=1200, BREAK_DURATION_MS=60000;
    public interface Persistence {
        Session load() throws Exception;
        void save(Session session) throws Exception;
    }
    private final Persistence storage;
    private final LongSupplier clock;
    private final Executor main, io, computer;
    private Runnable listener;
    private Consumer<PresentationEvent> presenter;
    private PresentationEvent presentation;
    private long generation=1, presentationDeadline=-1, replyAt=-1;
    public PresentationEvent presentation(){return presentation;}
    public void setPresenter(Consumer<PresentationEvent> value){
        presenter=value;
        if(value==null){cancelPresentation();main.execute(this::scheduleComputer);}
    }
    public void completePresentation(PresentationEvent.Id id){
        if(presentation==null||!presentation.id.equals(id))return;
        presentation=null;presentationDeadline=-1;
        replyAt=foreground&&!state.paused?clock.getAsLong()+100:-1;
        changed();
    }
    private void cancelPresentation(){presentation=null;presentationDeadline=-1;replyAt=-1;}
    public boolean previewSolvedDigit(int digit){
        return canMath()&&!state.solved&&MathInput.matches(MathInput.append(state.input,digit),state.exercise);
    }
    private final boolean ownExecutors;
    private Session state, failedSave;
    private ChessGame game;
    private boolean foreground, busy, pendingPause, corrupt, closed, workerRunning;
    private boolean mathsEnabled=true;
    private long anchor=-1, epoch, saveStarted;
    private long successAnchor=-1, successShownMs;
    private boolean successPending;
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
        successPending=state.phase==Session.Phase.MATH&&state.solved;
        try { game=ChessGame.fromHistory(state.moves); }
        catch(RuntimeException invalid) { game=new ChessGame(); corrupt=true; error="Het opgeslagen spel is ongeldig. Vraag een ouder om hulp."; }
        latestFocus=state.focus;
    }

    public Session state() { return state; }
    public boolean mathsEnabled() { return mathsEnabled; }
    public void setMathsEnabled(boolean enabled) { mathsEnabled=enabled; }
    /** Live foreground countdown; callers do not need to wait for a save checkpoint. */
    public long breakRemainingMs(){
        if(state.phase!=Session.Phase.BREAK)return 0;
        long elapsed=state.elapsedMs;
        if(active()&&anchor>=0)elapsed+=Math.max(0,clock.getAsLong()-anchor);
        return Math.max(0,BREAK_DURATION_MS-elapsed);
    }
    public ChessGame game() { return game; }
    public boolean busy() { return busy; }
    public String error() { return error; }
    public boolean corruptSave() { return corrupt; }
    public void setListener(Runnable listener) {
        this.listener=listener;
        if(listener==null){cancelPresentation();main.execute(this::scheduleComputer);}
    }
    private void changed() { if(listener!=null)listener.run(); }

    public void foreground(boolean visible) {
        if(closed)return;
        if(!visible)pause();
        foreground=visible;
    }
    public void resume() {
        if(!foreground||busy||error!=null||closed||presentation!=null)return;
        action(s->{
            if(!mathsEnabled&&s.phase!=Session.Phase.CHESS)beginChess(s);
            else if(s.phase==Session.Phase.TO_MATH)beginMath(s);
            else if(s.phase==Session.Phase.TO_CHESS)beginBreak(s);
            s.paused=false;
        },true);
    }
    public void continueAfterGame() {
        if(!canAct()||!foreground||state.paused||presentation!=null
                ||state.phase!=Session.Phase.CHESS||state.result.isEmpty())return;
        action(s->{if(mathsEnabled)beginMath(s);else beginChess(s);s.paused=false;},true);
    }
    public void pause() {
        if(closed)return;
        successAnchor=-1;successShownMs=0;
        cancelPresentation();
        epoch++; workerRunning=false;
        if(busy) { pendingPause=true; anchor=-1; return; }
        if(state.paused) { anchor=-1; return; }
        Session next=settled(); next.paused=true; commit(next);
    }

    public void tick() {
        if(closed)return;
        if(presentation!=null&&clock.getAsLong()>=presentationDeadline)completePresentation(presentation.id);
        if(replyAt>=0&&clock.getAsLong()>=replyAt){replyAt=-1;scheduleComputer();}
        if(busy) {
            if(clock.getAsLong()-saveStarted>2000 && error==null) {
                error="Opslaan duurt langer. De tijd staat stil."; changed();
            }
            return;
        }
        if(!active())return;
        if(successPending&&state.phase==Session.Phase.MATH&&state.solved
                &&successAnchor>=0&&successShownMs+Math.max(0,clock.getAsLong()-successAnchor)>=SUCCESS_DELAY_MS) {
            successPending=false;
            // The normal boundary check hands off instead of generating another
            // exercise when both round requirements have been met.
            action(this::generateExercise,false);return;
        }
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
        action(s->{
            s.input=MathInput.append(s.input,digit);s.feedback="";
            if(MathInput.matches(s.input,s.exercise)) {
                successPending=true;successShownMs=0;s.solved=true;s.correct++;s.feedback="Goed gedaan!";
            } else if(s.input.length()>=Integer.toString(s.exercise.answer()).length()) {
                s.feedback="Probeer nog eens.";
            }
        },false);
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
        successPending=false;
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
        epoch++; workerRunning=false;cancelPresentation();
        action(s->{if(mathsEnabled)beginMath(s);else beginChess(s);s.paused=true;},true);
    }
    public void skipToChess() {
        if(!canAct())return;
        epoch++; workerRunning=false;cancelPresentation();
        action(s->{s.phase=Session.Phase.CHESS;s.elapsedMs=0;s.chessPending=false;
            s.mathPending=false;s.paused=true;},true);
    }
    public void restartGame() {
        if(!canAct())return;
        epoch++;workerRunning=false;generation++;cancelPresentation();
        action(s->{
            if(s.phase!=Session.Phase.CHESS)s.elapsedMs=0;
            clearBoard(s);
            s.phase=Session.Phase.CHESS;s.paused=false;
            s.chessPending=false;s.mathPending=false;
        },true);
    }
    public void resetSession() {
        if(busy||closed)return;
        epoch++; workerRunning=false;generation++;cancelPresentation();
        Session fresh=new Session(false); fresh.config=state.config.copy();
        if(mathsEnabled)generateExercise(fresh);else beginChess(fresh);
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
        return canAct()&&presentation==null&&!state.paused&&foreground&&state.phase==Session.Phase.CHESS
                && state.result.isEmpty()&&game.whiteToMove()&&state.intendedMove==null;
    }
    private boolean canMath() {
        return canAct()&&!state.paused&&foreground&&state.phase==Session.Phase.MATH&&state.exercise!=null;
    }
    private boolean active() {
        return foreground&&!state.paused&&error==null&&!busy
                && ((state.phase==Session.Phase.CHESS && state.result.isEmpty()
                && state.intendedMove==null)||state.phase==Session.Phase.MATH||state.phase==Session.Phase.BREAK);
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
            // A chess game ends only by its result, never by foreground elapsed time.
            s.chessPending=false;
        } else if(s.phase==Session.Phase.MATH) {
            s.mathPending=s.elapsedMs>=300000;
            if(!successPending&&PhaseRules.mathComplete(s.elapsedMs,s.correct,s.solved))beginBreak(s);
        } else if(s.phase==Session.Phase.BREAK&&s.elapsedMs>=BREAK_DURATION_MS) {
            beginChess(s);
        }
    }
    private void beginChess(Session s){
        if(!s.result.isEmpty())clearBoard(s);
        s.phase=Session.Phase.CHESS;s.elapsedMs=0;s.chessPending=false;s.mathPending=false;
        successPending=false;successAnchor=-1;successShownMs=0;
    }
    private void beginBreak(Session s){
        s.phase=Session.Phase.BREAK;s.elapsedMs=0;s.chessPending=false;s.mathPending=false;
        successPending=false;successAnchor=-1;successShownMs=0;
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
        s.phase=Session.Phase.MATH;s.elapsedMs=0;s.correct=0;s.mathPending=false;s.chessPending=false;
        generateExercise(s);
    }
    private void generateExercise(Session s) {
        successPending=false;successAnchor=-1;successShownMs=0;
        MathGenerator generator=MathGenerator.restore(s.generatorState);
        s.exercise=generator.next(s.config);s.exerciseConfig=s.config.copy();
        s.generatorState=generator.snapshot();s.input="";s.hintLevel=0;s.solved=false;s.feedback="";
    }
    private void clearBoard(Session s) {
        s.moves=new ArrayList<>();s.result="";s.selected=-1;s.focus=52;s.promotionTo=-1;
        s.intendedMove=null;s.feedback="";
    }

    private void commit(Session next) {
        final boolean effectsAllowed=foreground&&!state.paused&&!next.paused;
        final boolean liveMove=foreground&&!state.paused&&next.moves.size()==state.moves.size()+1;
        if(next.focus!=state.focus)latestFocus=next.focus;
        next.revision=state.revision+1;
        busy=true;anchor=-1;saveStarted=clock.getAsLong();
        if(successAnchor>=0)successShownMs+=Math.max(0,clock.getAsLong()-successAnchor);
        successAnchor=-1;
        io.execute(()->{
            try {
                storage.save(next);
                main.execute(()->{
                    if(closed)return;
                    boolean boardChanged=!state.moves.equals(next.moves);
                    Session previous=state;
                    if(next.moves.size()<previous.moves.size())generation++;
                    state=next;
                    state.focus=latestFocus;
                    if(boardChanged)game=ChessGame.fromHistory(state.moves);
                    busy=false;error=null;failedSave=null;
                    if(pendingPause) {
                        pendingPause=false;Session paused=state.copy();paused.paused=true;commit(paused);return;
                    }
                    anchor=active()?clock.getAsLong():-1;
                    if(active()&&state.phase==Session.Phase.MATH&&state.solved&&successPending)
                        successAnchor=clock.getAsLong();
                    PresentationEvent event=foreground&&(effectsAllowed||liveMove)
                            ?PresentationEvent.committed(generation,previous,state):null;
                    if(event!=null&&presenter!=null){
                        if(event.kind==PresentationEvent.Kind.MOVE){
                            presentation=event;presentationDeadline=clock.getAsLong()+2000;
                        }
                        try{presenter.accept(event);}catch(RuntimeException ignored){cancelPresentation();}
                    }
                    changed();scheduleComputer();
                });
            } catch(Exception failure) {
                main.execute(()->{
                    if(closed)return;
                    busy=false;pendingPause=false;state.paused=true;anchor=-1;
                    successAnchor=-1;successShownMs=0;
                    failedSave=next.copy();failedSave.paused=true;
                    error="Opslaan is niet gelukt. De voortgang blijft staan. Probeer opnieuw.";
                    epoch++;workerRunning=false;changed();
                });
            }
        });
    }
    private void scheduleComputer() {
        if(presentation!=null||replyAt>=0||!canAct()||state.paused||!foreground||state.phase!=Session.Phase.CHESS
                ||!state.result.isEmpty()||game.whiteToMove()||workerRunning)return;
        long token=epoch,revision=state.revision;
        List<String> history=new ArrayList<>(state.moves);
        workerRunning=true;
        computer.execute(()->{
            String move;
            try {
                ChessGame board=ChessGame.fromHistory(history);
                if(board.claimableDraw())move="CLAIM";
                else {
                    move=ChessOpponent.choose(board);
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
        closed=true;epoch++;anchor=-1;cancelPresentation();presenter=null;
        if(ownExecutors) {
            ((ExecutorService)io).shutdown();((ExecutorService)computer).shutdownNow();
        }
    }
}
