package com.amine.chessmath;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlayerProfileTest {
    private Object p(String id,String name){return LocalChessTest.profile(id,name);}
    private String field(Object p,String name){try{return (String)p.getClass().getField(name).get(p);}catch(Exception e){throw new AssertionError(e);}}
    @Test public void trimsNameAndEmptyNameUsesCharacterDefault(){assertEquals("Amber",field(p("fox","  Amber  "),"customName"));assertNull(field(p("fox","  "),"customName"));}
    @Test public void acceptsTwentyFourSupplementaryCodePoints(){String s=new String(Character.toChars(0x1f431));String name=String.join("",java.util.Collections.nCopies(24,s));assertEquals(name,field(p("cat",name),"customName"));}
    @Test public void rejectsTwentyFiveCodePoints(){reject(String.join("",java.util.Collections.nCopies(25,"a")));}
    @Test public void rejectsControlCharacters(){for(String name:new String[]{"A\nB","A\tB","A\u0000B","A\u007fB"})reject(name);}
    private void reject(String name){try{Class.forName("com.amine.chessmath.PlayerProfile").getConstructor(String.class,String.class).newInstance("fox",name);fail("Invalid name accepted");}catch(java.lang.reflect.InvocationTargetException e){assertTrue(e.getCause() instanceof IllegalArgumentException);}catch(Exception e){throw new AssertionError("Profile contract missing",e);}}
    @Test public void portraitChangeKeepsExplicitCustomName(){Object first=p("fox","Amber");assertEquals("Amber",field(p("owl",field(first,"customName")),"customName"));}
}
