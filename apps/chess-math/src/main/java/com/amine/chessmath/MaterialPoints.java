package com.amine.chessmath;

/** Piece values derived from the displayed board, independent of engine evaluation. */
final class MaterialPoints {
    private MaterialPoints(){}
    static int total(ChessGame game,boolean white){return total(game::pieceAt,white);}
    static int before(PresentationEvent event,boolean white){return total(event::beforePiece,white);}
    static int after(PresentationEvent event,boolean white){return total(event::afterPiece,white);}
    private static int total(java.util.function.IntUnaryOperator pieces,boolean white){
        int total=0;
        for(int square=0;square<64;square++){
            char piece=(char)pieces.applyAsInt(square);
            if(Character.isUpperCase(piece)!=white)continue;
            switch(Character.toLowerCase(piece)){
                case 'p':total++;break;
                case 'n':case 'b':total+=3;break;
                case 'r':total+=5;break;
                case 'q':total+=9;break;
                default:break;
            }
        }
        return total;
    }
}
