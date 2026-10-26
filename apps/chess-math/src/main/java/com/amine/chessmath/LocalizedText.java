package com.amine.chessmath;

import java.util.Map;

/** Presentation boundary for human text in version-one Dutch snapshots. No game facts change. */
public final class LocalizedText {
    private LocalizedText(){}
    private static final Map<String,String> EN=new java.util.HashMap<>();
    static {
        EN.put("Zwart wint: schaakmat","Black wins: checkmate");
        EN.put("Wit wint: schaakmat","White wins: checkmate");
        EN.put("Remise: pat","Draw: stalemate");
        EN.put("Remise: onvoldoende materiaal","Draw: insufficient material");
        EN.put("Remise: vijfmaal dezelfde stelling","Draw: fivefold repetition");
        EN.put("Remise: 75 zetten zonder pionzet of slag","Draw: 75 moves without a pawn move or capture");
        EN.put("Remise geclaimd","Draw claimed");
        EN.put("Deze zet kan niet.","That move is not legal.");
        EN.put("Goed!","Correct!");EN.put("Goed gedaan!","Well done!");
        EN.put("Probeer nog eens.","Try again.");EN.put("Vul je antwoord in.","Enter your answer.");
        EN.put("Opslaan duurt langer. De tijd staat stil.","Saving is taking longer. The timer is paused.");
        EN.put("Opslaan is niet gelukt. De voortgang blijft staan. Probeer opnieuw.","Could not save. Your progress is kept. Try again.");
        EN.put("De computer kon geen zet kiezen. Pauzeer en probeer opnieuw.","The computer could not choose a move. Pause and try again.");
        EN.put("Het opgeslagen spel kan niet worden geopend. Een ouder kan opnieuw beginnen.","Could not open the saved game. An adult can reset progress.");
        EN.put("Het opgeslagen spel is ongeldig. Vraag een ouder om hulp.","The saved game is invalid. Ask an adult for help.");
        EN.put("Kies minstens één soort som.","Select at least one operation.");
        EN.put("Kies 20, 50 of 100 als getalgrens.","Select a number limit of 20, 50, or 100.");
        EN.put("Kies tafels van 1 tot en met 10.","Select tables from 1 to 10.");
        EN.put("Kies minstens één tafel of zet vermenigvuldigen uit.","Select at least one table or turn multiplication off.");
    }
    public static String message(String text,String language){
        if(text==null)return "";
        return "en".equals(language)?EN.getOrDefault(text,text):text;
    }
}
