package com.amine.chessmath;

import com.github.bhlangonijr.chesslib.move.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/** Strongest existing local search, with stable move ordering and balanced push/pop operations. */
public final class ChessOpponent {
    private static final int MATE=100000, MAX_NODES=60000, MAX_DEPTH=5;
    private static final long BUDGET_NS=800_000_000L;
    private ChessOpponent() { }

    public static String choose(ChessGame game){
        return choose(game,MAX_NODES,System::nanoTime);
    }
    static String choose(ChessGame game,int maxNodes,LongSupplier clock){
        Search search=new Search(maxNodes,clock,BUDGET_NS);
        ChessGame snapshot=game.copy();
        List<Move> moves=new ArrayList<>(snapshot.searchMoves());
        if(snapshot.searchOutcome(moves)!=0)return null;
        moves.sort((a,b)->a.toString().compareTo(b.toString()));
        order(snapshot,moves,true);
        Move best=moves.get(0);
        boolean completed=false;
        try {
            for(int depth=1;depth<=MAX_DEPTH;depth++){
                int bestScore=-MATE*2;
                Move iterationBest=best;
                moves.remove(best);moves.add(0,best);
                for(Move move:moves){
                    search.check();
                    snapshot.searchPush(move);
                    int score;
                    try {score=-search.score(snapshot,depth-1,-MATE*2,-bestScore,1);}
                    finally {snapshot.searchPop();}
                    if(score>bestScore){
                        bestScore=score;iterationBest=move;
                        // Before the first completed iteration, keep evaluated evidence instead of the ordered fallback.
                        if(!completed)best=move;
                    }
                    if(score==MATE-1)return move.toString();
                }
                best=iterationBest;completed=true;
            }
        } catch(BudgetExceeded stopped){ /* Keep completed search evidence when the budget expires. */ }
        return best.toString();
    }
    /** Stable ordering retains lexical ties; checks and captures reach tactical evidence first. */
    private static void order(ChessGame game,List<Move> moves,boolean checks){
        java.util.Map<Move,Integer> priorities=new java.util.HashMap<>();
        for(Move move:moves){
            int priority=captureValue(game,move.toString());
            int moving=pieceValue(game.pieceAt(square(move.toString(),0)));
            int captured=priority;
            if(checks){
                game.searchPush(move);
                try {
                    if(game.searchCheck())priority+=10000;
                    // A checking move must not make a hanging queen the deadline fallback.
                    if(game.searchDestinationAttacked(move)&&moving>captured)
                        priority-=20000+moving;
                    if(game.searchQueenAttacked())priority-=30000;
                }
                finally {game.searchPop();}
            }
            priorities.put(move,priority);
        }
        moves.sort((a,b)->Integer.compare(priorities.get(b),priorities.get(a)));
    }
    private static final class BudgetExceeded extends RuntimeException {
        @Override public synchronized Throwable fillInStackTrace(){return this;}
    }
    private static final class Search {
        final int maxNodes;
        final LongSupplier clock;
        final long start,budget;
        int nodes;
        Search(int maxNodes,LongSupplier clock,long budget){this.maxNodes=maxNodes;this.clock=clock;this.budget=budget;start=clock.getAsLong();}
        void check(){
            if(Thread.currentThread().isInterrupted()||nodes>=maxNodes||clock.getAsLong()-start>=budget)
                throw new BudgetExceeded();
            nodes++;
        }
        int score(ChessGame game,int depth,int alpha,int beta,int ply){
            check();
            List<Move> legal=game.searchMoves();
            int outcome=game.searchOutcome(legal);
            if(outcome!=0)return outcome<0?-MATE+ply:0;
            boolean draw=game.searchClaimableDraw();
            if(depth==0){
                int evaluation=evaluate(game)+captureOpportunity(game,legal);
                return draw?Math.max(0,evaluation):evaluation;
            }
            int best=draw?0:-MATE*2;
            if(draw)alpha=Math.max(alpha,0);
            order(game,legal,false);
            for(Move move:legal){
                check();
                game.searchPush(move);
                int value;
                try {value=-score(game,depth-1,-beta,-alpha,ply+1);}
                finally {game.searchPop();}
                best=Math.max(best,value);alpha=Math.max(alpha,value);
                if(alpha>=beta)break;
            }
            return best;
        }
    }
    private static int square(String move,int offset){
        return ('8'-move.charAt(offset+1))*8+move.charAt(offset)-'a';
    }
    /** Conservative immediate exchange gain prevents static leaves from rewarding a lost queen. */
    private static int captureOpportunity(ChessGame game,List<Move> legal){
        int best=0;
        for(Move move:legal){
            String uci=move.toString();
            int victim=pieceValue(game.pieceAt(square(uci,2)));
            int attacker=pieceValue(game.pieceAt(square(uci,0)));
            best=Math.max(best,victim-attacker);
        }
        return best;
    }
    private static int captureValue(ChessGame game,String move){
        int file=move.charAt(2)-'a',rank='8'-move.charAt(3);
        return pieceValue(game.pieceAt(rank*8+file))+(move.length()==5?pieceValue(move.charAt(4)):0);
    }
    private static int evaluate(ChessGame game){
        int score=0;
        for(int square=0;square<64;square++){
            char piece=game.pieceAt(square);
            if(piece=='.')continue;
            char type=Character.toLowerCase(piece);
            int value=pieceValue(piece);
            if(type!='k'){
                int row=square/8,column=square%8;
                int center=14-Math.abs(2*row-7)-Math.abs(2*column-7);
                value+=center*(type=='n'||type=='b'?3:1);
            }
            score+=Character.isUpperCase(piece)?value:-value;
        }
        return game.whiteToMove()?score:-score;
    }
    private static int pieceValue(char piece){
        switch(Character.toLowerCase(piece)){
            case 'p':return 100;
            case 'n':return 300;
            case 'b':return 320;
            case 'r':return 500;
            case 'q':return 900;
            default:return 0;
        }
    }
}
