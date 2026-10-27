package com.amine.chessmath;

/** A transient description of one durable action. Never reconstructed on restore. */
public final class PresentationEvent {
    public enum Kind { MOVE, MATH_SUCCESS }
    public enum Cue { MOVE, CAPTURE, CHECK, MATH_SUCCESS }
    public static final class Id {
        public final Kind kind;
        public final long generation, revision, subject;
        public final String move;
        private Id(Kind kind,long generation,long revision,long subject,String move) {
            this.kind=kind;this.generation=generation;this.revision=revision;this.subject=subject;this.move=move;
        }
        @Override public boolean equals(Object other) {
            if(!(other instanceof Id))return false;
            Id v=(Id)other;
            return kind==v.kind&&generation==v.generation&&revision==v.revision&&subject==v.subject&&move.equals(v.move);
        }
        @Override public int hashCode(){return java.util.Objects.hash(kind,generation,revision,subject,move);}
    }
    /** Monotonic identities reject duplicate renders and late callbacks without storing history. */
    public static final class Consumption {
        private long generation=Long.MIN_VALUE, revision=Long.MIN_VALUE;
        public boolean take(PresentationEvent e) {
            if(e.id.generation<generation||(e.id.generation==generation&&e.id.revision<=revision))return false;
            generation=e.id.generation;revision=e.id.revision;return true;
        }
    }
    public final Kind kind;
    public final Cue cue;
    public final Id id;
    public final String move, beforeLastMove, result;
    public final int from,to,capturedSquare,rookFrom,rookTo,beforeCheck,afterCheck,durationMs;
    public final char movingPiece,capturedPiece,promotionPiece,rookPiece;
    public final boolean beforeWhiteToMove;
    private final char[] before,after;
    private PresentationEvent(long generation,Session previous,Session next,boolean chess) {
        kind=chess?Kind.MOVE:Kind.MATH_SUCCESS;
        move=chess?next.moves.get(next.moves.size()-1):"";
        id=new Id(kind,generation,next.revision,chess?next.moves.size():next.exercise.id,move);
        result=next.result;
        beforeLastMove=previous.moves.isEmpty()?"":previous.moves.get(previous.moves.size()-1);
        before=new char[64];after=new char[64];
        if(!chess) {
            from=to=capturedSquare=rookFrom=rookTo=beforeCheck=afterCheck=-1;durationMs=0;
            movingPiece=capturedPiece=promotionPiece=rookPiece='.';beforeWhiteToMove=true;cue=Cue.MATH_SUCCESS;return;
        }
        ChessGame a=ChessGame.fromHistory(previous.moves),b=ChessGame.fromHistory(next.moves);
        for(int i=0;i<64;i++){before[i]=a.pieceAt(i);after[i]=b.pieceAt(i);}
        from=square(move,0);to=square(move,2);movingPiece=before[from];
        int capture=before[to]!='.'?to:-1;
        if(Character.toLowerCase(movingPiece)=='p'&&from%8!=to%8&&capture<0)capture=from/8*8+to%8;
        capturedSquare=capture;capturedPiece=capture<0?'.':before[capture];
        boolean castle=Character.toLowerCase(movingPiece)=='k'&&Math.abs(from-to)==2;
        rookFrom=castle?from/8*8+(to>from?7:0):-1;
        rookTo=castle?from+(to>from?1:-1):-1;
        rookPiece=castle?before[rookFrom]:'.';
        promotionPiece=move.length()==5?after[to]:'.';
        beforeCheck=a.checkSquare();afterCheck=b.checkSquare();beforeWhiteToMove=a.whiteToMove();
        int distance=Math.max(Math.abs(from/8-to/8),Math.abs(from%8-to%8));
        durationMs=castle?620:distance<=2?480:560;
        cue=capturedSquare>=0?Cue.CAPTURE:afterCheck>=0?Cue.CHECK:Cue.MOVE;
    }
    public char beforePiece(int square){return before[square];}
    public char afterPiece(int square){return after[square];}
    private static int square(String uci,int offset){return (8-(uci.charAt(offset+1)-'0'))*8+uci.charAt(offset)-'a';}
    public static PresentationEvent committed(long generation,Session before,Session after) {
        boolean move=after.moves.size()==before.moves.size()+1
                &&after.moves.subList(0,before.moves.size()).equals(before.moves);
        if(move)return new PresentationEvent(generation,before,after,true);
        if(before.phase==Session.Phase.MATH&&after.phase==Session.Phase.MATH&&!before.solved&&after.solved
                &&before.exercise!=null&&after.exercise!=null&&before.exercise.id==after.exercise.id)
            return new PresentationEvent(generation,before,after,false);
        return null;
    }
}
