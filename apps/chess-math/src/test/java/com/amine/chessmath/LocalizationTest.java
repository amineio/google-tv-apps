package com.amine.chessmath;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LocalizationTest {
    @Test public void englishHintsKeepOperandOrderAndBridgeTheCorrectTen(){
        Exercise crossing=new Exercise(1,28,7,Exercise.ADD);
        assertEquals("Split 7 into 2 and 5. First add 2 to 28. Then add the rest.",crossing.hint(1,"en"));
        Exercise subtract=new Exercise(2,32,7,Exercise.SUBTRACT);
        assertEquals("Split 7 into 2 and 5. First subtract 2 from 32. Then subtract the rest.",subtract.hint(1,"en"));
        assertEquals("Start at 30. Count back 7 steps. Use paper if you like.",new Exercise(3,30,7,Exercise.SUBTRACT).hint(1,"en"));
        assertEquals("Draw 3 groups of 5. Add the groups together.",new Exercise(4,3,5,Exercise.MULTIPLY).hint(1,"en"));
        assertEquals("32 − 7 = 25",subtract.hint(2,"en"));
        assertEquals(subtract.hint(1),subtract.hint(1,"nl"));
        assertEquals(25,subtract.answer());
    }
    @Test public void oldSavedResultsTranslateWithoutChangingTheirIdentity(){
        String savedResult="Remise: vijfmaal dezelfde stelling";
        assertEquals("Draw: fivefold repetition",LocalizedText.message(savedResult,"en"));
        assertEquals(savedResult,LocalizedText.message(savedResult,"nl"));
        assertEquals("Draw claimed",LocalizedText.message("Remise geclaimd","en"));
        assertEquals("White wins: checkmate",LocalizedText.message("Wit wint: schaakmat","en"));
        assertEquals("Black wins: checkmate",LocalizedText.message("Zwart wint: schaakmat","en"));
        assertEquals("Try again.",LocalizedText.message("Probeer nog eens.","en"));
        assertEquals("future diagnostic",LocalizedText.message("future diagnostic","en"));
    }
}
