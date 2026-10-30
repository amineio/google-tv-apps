package com.amine.chessmath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable local identity. Default names are resolved by the current UI locale. */
public final class PlayerProfile {
    public static final List<String> PORTRAITS=Collections.unmodifiableList(Arrays.asList("fox","owl","bear","cat","panda","rabbit","turtle","dragon"));
    public final String portraitId,customName;
    public PlayerProfile(String portraitId,String customName){
        if(!PORTRAITS.contains(portraitId))throw new IllegalArgumentException("Unknown portrait");
        String name=customName==null?null:trimName(customName);
        if(customName!=null&&customName.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Control characters are not allowed");
        if(name!=null&&name.codePointCount(0,name.length())>24)throw new IllegalArgumentException("Use at most 24 characters");
        this.portraitId=portraitId;this.customName=name==null||name.isEmpty()?null:name;
    }
    private static String trimName(String name){
        int start=0,end=name.length();
        while(start<end&&(Character.isWhitespace(name.codePointAt(start))||Character.isSpaceChar(name.codePointAt(start))))start+=Character.charCount(name.codePointAt(start));
        while(end>start&&(Character.isWhitespace(name.codePointBefore(end))||Character.isSpaceChar(name.codePointBefore(end))))end-=Character.charCount(name.codePointBefore(end));
        return name.substring(start,end);
    }
    static PlayerProfile restored(String id,String name,String excluded){
        if(!PORTRAITS.contains(id)||id.equals(excluded))for(String candidate:PORTRAITS)if(!candidate.equals(excluded)){id=candidate;break;}
        try{return new PlayerProfile(id,name);}
        catch(IllegalArgumentException invalidName){return new PlayerProfile(id,null);}
    }
}
