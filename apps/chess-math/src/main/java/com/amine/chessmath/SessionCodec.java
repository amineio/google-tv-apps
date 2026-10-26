package com.amine.chessmath;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** A versioned, validated local snapshot. A FEN alone is insufficient for repetition. */
public final class SessionCodec {
    private SessionCodec() {}

    public static String encode(Session s) throws JSONException {
        JSONObject j=new JSONObject();
        j.put("schema",1).put("id",s.id).put("revision",s.revision)
                .put("phase",s.phase.name()).put("paused",s.paused).put("elapsedMs",s.elapsedMs)
                .put("chessPending",s.chessPending).put("mathPending",s.mathPending)
                .put("solved",s.solved).put("correct",s.correct).put("selected",s.selected)
                .put("focus",s.focus).put("promotionTo",s.promotionTo).put("hintLevel",s.hintLevel)
                .put("intendedMove",s.intendedMove==null?JSONObject.NULL:s.intendedMove)
                .put("input",s.input).put("feedback",s.feedback).put("result",s.result)
                .put("moves",new JSONArray(s.moves)).put("config",config(s.config))
                .put("opponentSeed",s.opponentSeed).put("generator",s.generatorState);
        if(s.exercise!=null) {
            j.put("exercise",new JSONObject().put("id",s.exercise.id).put("a",s.exercise.a)
                    .put("b",s.exercise.b).put("operation",s.exercise.operation));
            j.put("exerciseConfig",config(s.exerciseConfig));
        }
        return j.toString();
    }

    public static Session decode(String raw) throws JSONException {
        if(raw.length()>2000000) throw new IllegalArgumentException("Snapshot too large");
        JSONObject j=new JSONObject(raw);
        require(j.getInt("schema")==1,"Unsupported save version");
        Session s=new Session();
        s.id=j.getString("id"); require(s.id.length()==36,"Invalid session id");
        s.revision=j.getLong("revision"); s.elapsedMs=j.getLong("elapsedMs");
        require(s.revision>=0 && s.elapsedMs>=0 && s.elapsedMs<=31536000000L,"Invalid elapsed time");
        s.phase=Session.Phase.valueOf(j.getString("phase")); s.paused=j.getBoolean("paused");
        s.chessPending=j.getBoolean("chessPending"); s.mathPending=j.getBoolean("mathPending");
        s.solved=j.getBoolean("solved"); s.correct=j.getInt("correct");
        require(s.correct>=0 && s.correct<=1000000,"Invalid answer count");
        s.selected=j.getInt("selected"); s.focus=j.getInt("focus");
        s.promotionTo=j.getInt("promotionTo"); s.hintLevel=j.getInt("hintLevel");
        require(square(s.selected,true) && square(s.focus,false) && square(s.promotionTo,true),"Invalid square");
        require(s.hintLevel>=0 && s.hintLevel<=2,"Invalid hint");
        s.intendedMove=j.isNull("intendedMove")?null:j.getString("intendedMove");
        s.input=j.getString("input"); s.feedback=j.getString("feedback"); s.result=j.getString("result");
        require(s.input.matches("[0-9]{0,3}") && (s.input.isEmpty()||Integer.parseInt(s.input)<=100),"Invalid answer input");
        require(s.feedback.length()<=300 && s.result.length()<=300,"Invalid message");
        JSONArray moves=j.getJSONArray("moves"); require(moves.length()<=20000,"Too many moves");
        for(int i=0;i<moves.length();i++) s.moves.add(moves.getString(i));
        ChessGame game=ChessGame.fromHistory(s.moves);
        if(!game.result().isEmpty()) require(s.result.equals(game.result()),"Missing game result");
        else require(s.result.isEmpty()||s.result.equals("Remise geclaimd"),"Invalid game result");
        if(s.selected>=0) require(game.whiteToMove() && Character.isUpperCase(game.pieceAt(s.selected)),"Invalid selection");
        if(s.promotionTo>=0) {
            require(s.selected>=0,"Promotion without selection");
            String prefix=uciSquare(s.selected)+uciSquare(s.promotionTo);
            require(game.legalMoves().stream().anyMatch(m->m.startsWith(prefix)&&m.length()==5),"Invalid promotion");
        }
        if(s.intendedMove!=null) require(game.claimableAfter(s.intendedMove),"Invalid intended claim");
        s.config=readConfig(j.getJSONObject("config")); s.opponentSeed=j.getLong("opponentSeed");
        s.generatorState=j.getString("generator"); MathGenerator.restore(s.generatorState);
        if(j.has("exercise")) {
            JSONObject e=j.getJSONObject("exercise");
            s.exercise=new Exercise(e.getLong("id"),e.getInt("a"),e.getInt("b"),e.getString("operation"));
            s.exerciseConfig=readConfig(j.getJSONObject("exerciseConfig"));
            validateExercise(s.exercise,s.exerciseConfig);
        }
        if(s.phase==Session.Phase.MATH||s.phase==Session.Phase.TO_CHESS) require(s.exercise!=null,"Missing exercise");
        if(s.solved) require(s.exercise!=null && s.correct>0 && MathInput.matches(s.input,s.exercise),"Invalid completed answer");
        if(s.phase==Session.Phase.TO_CHESS) require(PhaseRules.mathComplete(s.elapsedMs,s.correct,s.solved),"Incomplete math handoff");
        if(s.phase==Session.Phase.TO_MATH) require(s.elapsedMs>=300000 && (game.whiteToMove()||!s.result.isEmpty()) && s.selected<0 && s.promotionTo<0,"Unstable chess handoff");
        return s;
    }

    private static void validateExercise(Exercise e,MathConfig c) {
        require(e.id>=0 && e.a>=0 && e.b>=0 && e.a<=100 && e.b<=100,"Invalid operands");
        switch(e.operation) {
            case "ADD": require(c.addition && e.a<=c.limit && e.b<=c.limit && e.answer()<=c.limit
                    && (c.crossingTen||e.a%10+e.b%10<10),"Invalid addition"); break;
            case "SUBTRACT": require(c.subtraction && e.a<=c.limit && e.b<=e.a
                    && (c.crossingTen||e.a%10>=e.b%10),"Invalid subtraction"); break;
            case "MULTIPLY": require(c.multiplication && e.a>=1 && e.a<=10 && e.b>=1 && e.b<=10 && c.tables[e.b],"Invalid table"); break;
            default: throw new IllegalArgumentException("Invalid operation");
        }
    }

    private static JSONObject config(MathConfig c) throws JSONException {
        JSONArray tables=new JSONArray();
        for(int i=1;i<=10;i++) tables.put(c.tables[i]);
        return new JSONObject().put("addition",c.addition).put("subtraction",c.subtraction)
                .put("multiplication",c.multiplication).put("crossingTen",c.crossingTen)
                .put("limit",c.limit).put("tables",tables);
    }
    private static MathConfig readConfig(JSONObject j) throws JSONException {
        MathConfig c=new MathConfig(); c.addition=j.getBoolean("addition");
        c.subtraction=j.getBoolean("subtraction"); c.multiplication=j.getBoolean("multiplication");
        c.crossingTen=j.getBoolean("crossingTen"); c.limit=j.getInt("limit");
        JSONArray t=j.getJSONArray("tables"); require(t.length()==10,"Invalid table selection");
        for(int i=1;i<=10;i++)c.tables[i]=t.getBoolean(i-1);
        c.validate(); return c;
    }
    private static boolean square(int n,boolean none) { return n>= (none?-1:0) && n<64; }
    static String uciSquare(int square) { return ""+(char)('a'+square%8)+(8-square/8); }
    private static void require(boolean ok,String message) { if(!ok)throw new IllegalArgumentException(message); }
}
