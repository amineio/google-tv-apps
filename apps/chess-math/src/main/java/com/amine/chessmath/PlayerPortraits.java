package com.amine.chessmath;

import android.content.Context;

/** App resource boundary; no drawable resource IDs are persisted. */
final class PlayerPortraits {
    private PlayerPortraits(){}
    static int image(String id){
        switch(id){
            case "owl":return R.drawable.profile_owl;
            case "bear":return R.drawable.profile_bear;
            case "cat":return R.drawable.profile_cat;
            case "panda":return R.drawable.profile_panda;
            case "rabbit":return R.drawable.profile_rabbit;
            case "turtle":return R.drawable.profile_turtle;
            case "dragon":return R.drawable.profile_dragon;
            default:return R.drawable.profile_fox;
        }
    }
    static String name(Context context,PlayerProfile profile){
        if(profile.customName!=null)return profile.customName;
        int id;
        switch(profile.portraitId){
            case "owl":id=R.string.profile_owl;break;
            case "bear":id=R.string.profile_bear;break;
            case "cat":id=R.string.profile_cat;break;
            case "panda":id=R.string.profile_panda;break;
            case "rabbit":id=R.string.profile_rabbit;break;
            case "turtle":id=R.string.profile_turtle;break;
            case "dragon":id=R.string.profile_dragon;break;
            default:id=R.string.profile_fox;
        }
        return context.getString(id);
    }
    static String seat(Context context,PlayerProfile profile,boolean white){
        return context.getString(R.string.local_seat,name(context,profile),context.getString(white?R.string.local_white:R.string.local_black));
    }
}
