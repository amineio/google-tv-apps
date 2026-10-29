package com.amine.asktv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

val Gold=Color(0xFFF6CD83)
val Background=Color(0xFF0D0E12)
val Ink=Color(0xFFF8F5F0)
val Muted=Color(0xFFB4B0AC)
val Panel=Color(0xFF222329)
@OptIn(ExperimentalTextApi::class)
val Outfit=FontFamily(*listOf(FontWeight.Normal,FontWeight.Medium,FontWeight.SemiBold,FontWeight.Bold).map {
    Font(R.font.outfit,weight=it,variationSettings=FontVariation.Settings(FontVariation.weight(it.weight)))
}.toTypedArray())
@Composable fun AskTheme(content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Gold,background=Background,onBackground=Ink,surface=Panel,onSurface=Ink)) {
        CompositionLocalProvider(LocalContentColor provides Ink,LocalTextStyle provides LocalTextStyle.current.copy(fontFamily=Outfit),content=content)
    }
}
@Composable fun TvButton(text:String,modifier:Modifier=Modifier,primary:Boolean=false,enabled:Boolean=true,onClick:()->Unit) {
    val shape=RoundedCornerShape(14.dp)
    Button(onClick,modifier.heightIn(min=46.dp),enabled=enabled,shape=ButtonDefaults.shape(shape),
        colors=ButtonDefaults.colors(containerColor=Panel,contentColor=Ink,
            focusedContainerColor=Gold,focusedContentColor=Background),
        scale=ButtonDefaults.scale(focusedScale=1.025f),
        border=ButtonDefaults.border(border=Border(BorderStroke(1.dp,if(primary && enabled) Gold.copy(alpha=.55f) else Ink.copy(alpha=.16f)),shape=shape),
            focusedBorder=Border(BorderStroke(3.dp,Gold),shape=shape)),
        contentPadding=PaddingValues(horizontal=22.dp,vertical=12.dp)) {
        Text(text,fontFamily=Outfit,fontSize=19.sp,maxLines=2)
    }
}
