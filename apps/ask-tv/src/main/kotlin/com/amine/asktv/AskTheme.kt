package com.amine.asktv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
internal val LocalControlFocusVisible=staticCompositionLocalOf { true }
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
    var focused by remember { mutableStateOf(false) }
    // Bind appearance to the actual focus node rather than relying on Surface's
    // interaction collector to receive entry focus. Keep native input/press handling.
    val showFocus=focused && LocalControlFocusVisible.current && enabled
    val container=if(showFocus) Gold else Panel
    val content=if(showFocus) Background else Ink
    val outline=Border(BorderStroke(if(showFocus) 3.dp else 1.dp,if(showFocus) Gold else Ink.copy(alpha=.16f)),shape=shape)
    val focusScale=if(showFocus) 1.025f else 1f
    Button(onClick,modifier.onFocusChanged { focused=it.isFocused }.heightIn(min=46.dp),enabled=enabled,shape=ButtonDefaults.shape(shape),
        colors=ButtonDefaults.colors(containerColor=container,contentColor=content,
            focusedContainerColor=container,focusedContentColor=content),
        scale=ButtonDefaults.scale(scale=focusScale,focusedScale=focusScale,pressedScale=1f),
        border=ButtonDefaults.border(border=outline,focusedBorder=outline),
        contentPadding=PaddingValues(horizontal=22.dp,vertical=12.dp)) {
        Text(text,fontFamily=Outfit,fontSize=19.sp,maxLines=2)
    }
}
