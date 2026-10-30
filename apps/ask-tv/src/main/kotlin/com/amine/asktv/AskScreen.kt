package com.amine.asktv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

@Composable fun AskScreen(model:AskViewModel,onAsk:()->Unit,onExit:()->Unit) {
    val ui=model.ui
    var typing by rememberSaveable { mutableStateOf(false) }
    val focus=remember { listOf("ask","type","configuration","example-0","example-1","answer-scroll","sign-in","new-conversation","follow-up","ready-sign-in").associateWith { FocusRequester() } }
    fun requester(name:String)=focus.getValue(name)
    LaunchedEffect(ui.page,ui.focusVersion) { if(!typing) runCatching { requester(ui.focus).requestFocus() } }
    BackHandler { if(typing) { typing=false; model.dialogClosed() } else if(!model.back()) onExit() }
    Box(Modifier.fillMaxSize().background(Background)) {
        Image(painterResource(R.drawable.ask_hero),null,Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Crop,alpha=.75f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Background,Background.copy(alpha=.75f),Background.copy(alpha=.05f)))))
        Column(Modifier.fillMaxSize().padding(horizontal=52.dp,vertical=28.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ask_icon),null,Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
                Text("AI Chat",fontFamily=Outfit,fontSize=25.sp,fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if(ui.page!=Page.SETTINGS) TvButton("Configuration",Modifier.testTag("configuration").focusRequester(requester("configuration")).onFocusChanged { if(it.isFocused) model.focused("configuration") }
                    .focusProperties { down=requester(if(ui.page==Page.READY) "ask" else "answer-scroll") },onClick=model::settings)
            }
            when(ui.page) {
                Page.READY -> {
                    Spacer(Modifier.weight(1f))
                    Text("What are you\ncurious about?",fontFamily=Outfit,fontSize=42.sp,lineHeight=47.sp,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text(ui.busy ?: "Ask a question. Hear the answer.",color=Muted,fontSize=20.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    TvButton(if(ui.busy=="Listening…") "Stop listening" else "Ask a question",Modifier.width(240.dp).testTag("ask").focusRequester(requester("ask")).onFocusChanged { if(it.isFocused) model.focused("ask") }
                        .focusProperties { up=requester("configuration"); down=requester("type") },primary=true,
                        onClick={if(ui.busy!=null) model.stop() else onAsk()})
                    TvButton("Type a question",Modifier.width(240.dp).testTag("type").focusRequester(requester("type")).onFocusChanged { if(it.isFocused) model.focused("type") }
                        .focusProperties { up=requester("ask"); down=requester("example-0") },onClick={model.stop();typing=true})
                    if(!ui.planAllowed) TvButton(if(ui.signedIn) "Enable plan" else "Sign in",Modifier.testTag("ready-sign-in").focusRequester(requester("ready-sign-in"))
                        .onFocusChanged { if(it.isFocused) model.focused("ready-sign-in") },onClick={if(ui.signedIn) model.enablePlan() else model.signIn()})
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(when { ui.planAllowed -> "Microphone off until you ask"; ui.signedIn -> "Enable ChatGPT plan for live answers · Microphone off"; else -> "Sign in for live answers · Microphone off" },color=Muted,fontSize=16.sp)
                    Spacer(Modifier.height(20.dp))
                    Text("TRY AN EXAMPLE",color=Muted,fontSize=13.sp,letterSpacing=2.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        EXAMPLES.forEachIndexed { index,example ->
                            TvButton(if(index==0) example.first else "A simple dinner idea",Modifier.width(324.dp).height(52.dp).testTag("example-$index").focusRequester(requester("example-$index")).onFocusChanged { if(it.isFocused) model.focused("example-$index") }
                                .focusProperties { up=requester("type"); if(index==0) right=requester("example-1") else left=requester("example-0") },onClick={model.example(index)})
                        }
                    }
                    Spacer(Modifier.weight(.6f))
                    Status(ui)
                }
                Page.ANSWER -> {
                    Spacer(Modifier.height(22.dp))
                    Text(if(ui.example) "Example preview · not connected" else if(ui.incomplete) "${ui.busy ?: "Incomplete answer"}" else "ChatGPT · ${ui.models.firstOrNull{it.slug==ui.model}?.displayName ?: "Subscription"}",color=Gold,fontSize=16.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(ui.question,fontFamily=Outfit,fontSize=29.sp,fontWeight=FontWeight.SemiBold,maxLines=2)
                    Spacer(Modifier.height(15.dp))
                    val scroll=rememberScrollState()
                    val scope=rememberCoroutineScope()
                    var scrollFocused by remember { mutableStateOf(false) }
                    LaunchedEffect(ui.question) { scroll.scrollTo(0) }
                    Box(Modifier.weight(1f).fillMaxWidth().padding(end=52.dp).clip(RoundedCornerShape(14.dp))
                        .background(Panel.copy(alpha=.75f)).border(if(scrollFocused) 2.dp else 1.dp,if(scrollFocused) Gold else Ink.copy(alpha=.12f),RoundedCornerShape(14.dp))
                        .testTag("answer-scroll").focusRequester(requester("answer-scroll")).onFocusChanged { if(it.isFocused) model.focused("answer-scroll") }.focusProperties { up=requester("configuration") }
                        .onFocusChanged { scrollFocused=it.isFocused }.onPreviewKeyEvent { event ->
                            if(event.type==KeyEventType.KeyDown && event.key==Key.DirectionDown && scroll.value<scroll.maxValue) { scope.launch{scroll.scrollTo((scroll.value+100).coerceAtMost(scroll.maxValue))};true }
                            else if(event.type==KeyEventType.KeyDown && event.key==Key.DirectionUp && scroll.value>0) { scope.launch{scroll.scrollTo((scroll.value-100).coerceAtLeast(0))};true } else false
                        }.focusable().drawWithContent {
                            drawContent()
                            if(scroll.maxValue>0) {
                                if(scroll.value<scroll.maxValue) {
                                    val fade=40.dp.toPx().coerceAtMost(size.height)
                                    drawRect(Brush.verticalGradient(listOf(Color.Transparent,Panel),
                                        startY=size.height-fade,endY=size.height),
                                        topLeft=Offset(2.dp.toPx(),size.height-fade),size=Size(size.width-4.dp.toPx(),fade))
                                }
                                val top=12.dp.toPx()
                                val track=(size.height-2*top).coerceAtLeast(1f)
                                val width=3.dp.toPx()
                                val x=size.width-10.dp.toPx()
                                val viewport=scroll.viewportSize.toFloat().coerceAtLeast(1f)
                                val thumb=(track*viewport/(scroll.maxValue+viewport)).coerceIn(24.dp.toPx().coerceAtMost(track),track)
                                drawRoundRect(Ink.copy(alpha=.16f),Offset(x,top),Size(width,track),CornerRadius(width))
                                drawRoundRect(Gold.copy(alpha=.9f),Offset(x,top+(track-thumb)*scroll.value/scroll.maxValue),Size(width,thumb),CornerRadius(width))
                            }
                        }) {
                        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(22.dp)) {
                            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                ui.answer.ifBlank { "Waiting for an answer…" }.split("\n\n").forEach { paragraph ->
                                    Text(paragraph,fontFamily=Outfit,fontSize=23.sp,lineHeight=32.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.padding(end=60.dp)) {
                        ui.stopLabel?.let { label -> TvButton(label,onClick=model::stop) }
                        TvButton("Read aloud",enabled=ui.answer.isNotBlank()&&!ui.incomplete,onClick=model::repeatAnswer)
                        TvButton("Follow-up",Modifier.testTag("follow-up").focusRequester(requester("follow-up")).onFocusChanged { if(it.isFocused) model.focused("follow-up") },onClick={model.stop();typing=true})
                        TvButton("New conversation",Modifier.testTag("new-conversation").focusRequester(requester("new-conversation")).onFocusChanged { if(it.isFocused) model.focused("new-conversation") },onClick=model::newConversation)
                    }
                    Spacer(Modifier.height(10.dp))
                    Status(ui)
                }
                Page.SETTINGS -> {
                    Spacer(Modifier.height(22.dp))
                    Text("Configuration",fontFamily=Outfit,fontSize=34.sp,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.weight(1f).fillMaxWidth().padding(end=140.dp).verticalScroll(rememberScrollState()).padding(bottom=8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(when { ui.planAllowed -> "ChatGPT subscription connected"; ui.signedIn -> "ChatGPT plan permission needed"; else -> "Sign in to use your ChatGPT subscription" },color=Ink,fontSize=22.sp)
                        if(ui.signedIn) Text(ui.account,color=Muted,fontSize=18.sp)
                        Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            TvButton(if(ui.signedIn && !ui.planAllowed) "Enable ChatGPT plan" else "Continue with ChatGPT",Modifier.testTag("sign-in").focusRequester(requester("sign-in")).onFocusChanged { if(it.isFocused) model.focused("sign-in") },primary=true,onClick={if(ui.signedIn && !ui.planAllowed) model.enablePlan() else model.signIn()})
                            if(ui.signedIn) TvButton("Sign out",onClick=model::signOut)
                            if(ui.busy!=null) TvButton("Cancel",onClick=model::stop)
                        }
                        Text(if(ui.signedIn) "Model · "+(ui.models.firstOrNull{it.slug==ui.model}?.displayName ?: "Load available models") else "Available models appear after sign-in.",color=Muted,fontSize=18.sp)
                        if(ui.planAllowed) {
                            TvButton("Refresh available models",onClick=model::refreshModels)
                            ui.models.forEach { entry -> TvButton((if(entry.slug==ui.model) "✓  " else "")+entry.displayName,onClick={model.chooseModel(entry.slug)}) }
                        }
                        TvButton("Read answers aloud · "+if(ui.speech) "On" else "Off",onClick=model::toggleSpeech)
                        Text("Answers use an installed Android voice.",color=Muted,fontSize=17.sp,lineHeight=23.sp)
                        Text("If microphone input is unavailable, type your question.",color=Muted,fontSize=17.sp,lineHeight=23.sp)
                        Text("New conversation clears the conversation in memory.",color=Muted,fontSize=17.sp,lineHeight=23.sp)
                        TvButton("Back",onClick={model.back()})
                    }
                    Spacer(Modifier.height(10.dp))
                    Status(ui)
                }
            }
        }
    }
    if(typing) {
        var text by rememberSaveable { mutableStateOf("") }
        val inputFocus=remember { FocusRequester() }
        Dialog(onDismissRequest={typing=false;model.dialogClosed()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
            Box(Modifier.fillMaxSize().imePadding().padding(24.dp),contentAlignment=Alignment.TopCenter) {
            Column(Modifier.widthIn(max=640.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp,Gold.copy(alpha=.55f),RoundedCornerShape(22.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(if(ui.page==Page.ANSWER) "Ask a follow-up" else "Type a question",fontFamily=Outfit,fontSize=26.sp,color=Ink)
                BasicTextField(text,{if(it.length<=4000)text=it},Modifier.fillMaxWidth().height(52.dp).background(Background,RoundedCornerShape(12.dp)).padding(horizontal=14.dp,vertical=11.dp).testTag("question-input").focusRequester(inputFocus),
                    singleLine=true,textStyle=TextStyle(color=Ink,fontFamily=Outfit,fontSize=22.sp),cursorBrush=androidx.compose.ui.graphics.SolidColor(Gold))
                Text("${text.length} / 4,000",color=Muted,fontSize=14.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    TvButton("Ask",primary=true,enabled=text.isNotBlank(),onClick={typing=false;model.submit(text)})
                    TvButton("Cancel",onClick={typing=false;model.dialogClosed()})
                }
            }
            }
        }
        LaunchedEffect(Unit) { inputFocus.requestFocus() }
    }
}
@Composable private fun Status(ui:AskUi) {
    Text(ui.message.ifBlank { ui.busy ?: when(ui.page) { Page.READY -> "OK  Select · Back  Exit"; Page.ANSWER -> "Up / Down  Scroll · Back  Return"; Page.SETTINGS -> "Back  Return" } },
        modifier=Modifier.fillMaxWidth().padding(end=120.dp).heightIn(min=30.dp),color=if(ui.message.isNotBlank()) Gold else Muted,fontSize=15.sp,lineHeight=21.sp,maxLines=3)
}
