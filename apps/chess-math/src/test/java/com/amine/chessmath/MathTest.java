package com.amine.chessmath;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MathTest {
    @Test public void configRejectsEmptyFamiliesAndTablesAndCopiesChoices() {
        MathConfig c = new MathConfig();
        assertEquals(100, c.limit);
        assertFalse(c.crossingTen);
        assertTrue(c.tables[1] && c.tables[2] && c.tables[5] && c.tables[10]);
        MathConfig copy = c.copy(); copy.tables[5] = false;
        assertTrue(c.tables[5]);
        c.addition = c.subtraction = c.multiplication = false;
        assertThrows(IllegalArgumentException.class, c::validate);
        c.multiplication = true; Arrays.fill(c.tables, false);
        assertThrows(IllegalArgumentException.class, c::validate);
        c.multiplication = false; c.addition = true; c.validate();
        c.limit = 21; assertThrows(IllegalArgumentException.class, c::validate);
    }
    @Test public void generatedFamiliesRotateWithoutSuppressingTables() {
        MathGenerator g = new MathGenerator(9); MathConfig c = new MathConfig();
        for (int i = 0; i < 100; i++) {
            Set<String> families = new HashSet<>();
            for (int j = 0; j < 3; j++) families.add(g.next(c).operation);
            assertEquals(3, families.size());
        }
    }
    @Test public void allAdditionAndSubtractionCandidatesRespectBoundsAndCrossing() {
        for (int limit : new int[]{20,50,100}) for (boolean crossing : new boolean[]{false,true}) {
            MathConfig c = new MathConfig(); c.limit = limit; c.crossingTen = crossing; c.multiplication = false;
            MathGenerator g = new MathGenerator(22); Set<String> seen = new HashSet<>();
            for (int i = 0; i < 15000; i++) {
                Exercise e = g.next(c); seen.add(e.expression());
                assertTrue(e.a >= 0 && e.a <= limit && e.b >= 0 && e.b <= limit);
                assertTrue(e.answer() >= 0 && e.answer() <= limit);
                if (!crossing && e.operation.equals("ADD")) assertTrue(e.a % 10 + e.b % 10 < 10);
                if (!crossing && e.operation.equals("SUBTRACT")) assertTrue(e.a % 10 >= e.b % 10);
            }
            assertTrue(seen.contains("20 + 0")); assertTrue(seen.contains("20 − 0"));
            if (crossing) { assertTrue(seen.contains("8 + 2")); assertTrue(seen.contains("20 − 1")); }
            else { assertFalse(seen.contains("8 + 2")); assertFalse(seen.contains("20 − 1")); }
        }
    }
    @Test public void multiplicationUsesEveryMultiplierOfSelectedTableDespiteLimit() {
        MathConfig c = new MathConfig(); c.addition = c.subtraction = false; c.limit = 20;
        Arrays.fill(c.tables, false); c.tables[5] = true;
        MathGenerator g = new MathGenerator(8); String previous = "";
        for (int round = 0; round < 20; round++) {
            Set<Integer> multipliers = new HashSet<>();
            for (int i = 0; i < 10; i++) {
                Exercise e = g.next(c); assertEquals(5,e.b); multipliers.add(e.a);
                assertNotEquals(previous,e.expression()); previous=e.expression();
            }
            assertEquals(10,multipliers.size());
        }
    }
    @Test public void restoreContinuesBagsIdsAndConfigurationChangesExactly() {
        MathGenerator g = new MathGenerator(Long.MIN_VALUE); MathConfig c = new MathConfig();
        for (int i=0;i<37;i++) g.next(c);
        MathGenerator restored = MathGenerator.restore(g.snapshot());
        for (int i=0;i<500;i++) {
            if(i==100) { c.addition=false; c.limit=20; c.tables[3]=true; }
            Exercise a=g.next(c), b=restored.next(c);
            assertEquals(a.id,b.id); assertEquals(a.expression(),b.expression());
            assertEquals(g.snapshot(),restored.snapshot());
        }
        assertThrows(IllegalArgumentException.class, () -> MathGenerator.restore("garbage"));
    }
    @Test public void invalidConfigurationDoesNotConsumeState() {
        MathGenerator g=new MathGenerator(3); String before=g.snapshot(); MathConfig c=new MathConfig();
        c.addition=c.subtraction=c.multiplication=false;
        assertThrows(IllegalArgumentException.class, () -> g.next(c)); assertEquals(before,g.snapshot());
    }
    @Test public void expressionsAndWorkedHintsUseStoredOperands() {
        Exercise add=new Exercise(1,28,7,"ADD"), sub=new Exercise(2,30,1,"SUBTRACT"), mul=new Exercise(3,3,5,"MULTIPLY");
        assertEquals(35,add.answer()); assertEquals(29,sub.answer()); assertEquals(15,mul.answer());
        assertEquals("3 × 5",mul.expression());
        assertTrue(add.hint(1).contains("2")); assertTrue(add.hint(1).contains("5"));
        assertTrue(sub.hint(1).contains("1")); assertFalse(sub.hint(1).contains("29"));
        assertTrue(mul.hint(1).contains("3")); assertTrue(mul.hint(1).contains("5"));
        assertTrue(add.hint(2).contains("35")); assertTrue(sub.hint(2).contains("29")); assertTrue(mul.hint(2).contains("15"));
        assertThrows(IllegalArgumentException.class, () -> new Exercise(1,1,2,"DIVIDE"));
    }
    @Test public void additionHintsUseTensAndUnitsUnlessUnitsCrossTen() {
        assertTrue(new Exercise(1,32,24,"ADD").hint(1).contains("Splits 24 in 20 en 4."));
        assertTrue(new Exercise(2,34,28,"ADD").hint(1).contains("Splits 28 in 6 en 22."));
        assertTrue(new Exercise(3,38,2,"ADD").hint(1).contains("Splits 2 in 2 en 0."));
    }
    @Test public void subtractionHintsUseTensAndUnitsUnlessUnitsNeedBorrowing() {
        assertTrue(new Exercise(1,74,12,"SUBTRACT").hint(1).contains("Splits 12 in 10 en 2."));
        assertTrue(new Exercise(2,74,18,"SUBTRACT").hint(1).contains("Splits 18 in 4 en 14."));
        assertTrue(new Exercise(3,74,14,"SUBTRACT").hint(1).contains("Splits 14 in 10 en 4."));
    }
    @Test public void numericEntryIsBoundedAndNormalizesZeros() {
        assertEquals("0",MathInput.append("0",0)); assertEquals("9",MathInput.append("0",9));
        assertEquals("100",MathInput.append("10",0)); assertEquals("10",MathInput.append("10",1));
        assertEquals("100",MathInput.append("100",0)); assertEquals("",MathInput.delete("1"));
        assertEquals("10",MathInput.delete("100")); assertEquals("",MathInput.delete(""));
        Exercise e=new Exercise(1,20,10,"ADD"); assertTrue(MathInput.matches("030",e));
        assertFalse(MathInput.matches("",e)); assertFalse(MathInput.matches("-1",e)); assertFalse(MathInput.matches("junk",e));
        assertThrows(IllegalArgumentException.class, () -> MathInput.append("",10));
    }
}
