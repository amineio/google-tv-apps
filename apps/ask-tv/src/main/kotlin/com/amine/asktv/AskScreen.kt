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
import androidx.compose.ui.platform.LocalDensity
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

@OptIn(ExperimentalLayoutApi::class)
@Composable fun AskScreen(model:AskViewModel,onAsk:()->Unit,onExit:()->Unit,onPhoneSignIn:(Boolean)->Unit) {
    val ui=model.ui
    val focusManager=androidx.compose.ui.platform.LocalFocusManager.current
    var typing by rememberSaveable { mutableStateOf(false) }
    val focus=remember { listOf("ask","type","configuration","example-0","example-1","answer-scroll","sign-in","new-conversation","follow-up","ready-sign-in","change-model","models-refresh","reconnect").associateWith { FocusRequester() } }
    fun requester(name:String)=focus.getValue(name)
    LaunchedEffect(ui.page,ui.focusVersion) {
        // Let newly composed TV Surface interaction collectors subscribe before entry focus.
        withFrameNanos { }
        if(!typing) runCatching { requester(ui.focus).requestFocus() }
    }
    BackHandler { if(typing) { typing=false; model.dialogClosed() } else if(!model.back()) onExit() }
    CompositionLocalProvider(LocalControlFocusVisible provides !typing) {
    Box(Modifier.fillMaxSize().background(Background)) {
        Image(painterResource(R.drawable.ask_hero),null,Modifier.fillMaxSize(),contentScale=androidx.compose.ui.layout.ContentScale.Crop,alpha=.75f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Background,Background.copy(alpha=.75f),Background.copy(alpha=.05f)))))
        Column(Modifier.fillMaxSize().padding(horizontal=52.dp,vertical=28.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ask_icon),null,Modifier.size(36.dp))
                Spacer(Modifier.width(12.dp))
                Text("AI Chat",fontFamily=Outfit,fontSize=25.sp,fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if(ui.page==Page.READY || ui.page==Page.ANSWER) TvButton("Configuration",Modifier.testTag("configuration").focusRequester(requester("configuration")).onFocusChanged { if(it.isFocused) model.focused("configuration") }
                    .focusProperties { down=requester(if(ui.page==Page.READY) (if(ui.planAllowed) "ask" else "ready-sign-in") else "answer-scroll") },onClick=model::settings)
            }
            when(ui.page) {
                Page.READY -> {
                    Spacer(Modifier.weight(1f))
                    Text("What are you\ncurious about?",fontFamily=Outfit,fontSize=42.sp,lineHeight=47.sp,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text(ui.busy ?: "Ask a question. Hear the answer.",color=Muted,fontSize=20.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    val primary=if(ui.planAllowed) "ask" else "ready-sign-in"
                    TvButton(if(!ui.planAllowed) { if(ui.signedIn) "Allow plan access" else "Continue with ChatGPT" } else if(ui.busy=="Listening…") "Stop listening" else "Ask a question",
                        Modifier.width(if(ui.planAllowed) 240.dp else 290.dp).testTag(primary).focusRequester(requester(primary)).onFocusChanged { if(it.isFocused) model.focused(primary) }
                            .focusProperties { up=requester("configuration"); down=requester("type"); right=requester("type") },
                        onClick={if(!ui.planAllowed) model.chooseSignInRoute(ui.signedIn) else if(ui.busy!=null) model.stop() else onAsk()})
                    TvButton("Type a question",Modifier.width(240.dp).testTag("type").focusRequester(requester("type")).onFocusChanged { if(it.isFocused) model.focused("type") }
                        .focusProperties { up=requester(primary); left=requester(primary); down=requester("example-0") },onClick={model.stop();focusManager.clearFocus(force=true);typing=true})
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(when { ui.busy=="Listening…" -> "Microphone on · Listening"; ui.planAllowed -> "Microphone off · Starts when you ask"; ui.signedIn -> "Allow plan access for live answers · Microphone off"; else -> "Sign in for live answers · Microphone off" },color=Muted,fontSize=16.sp)
                    Spacer(Modifier.height(20.dp))
                    Text("EXAMPLE PREVIEWS",color=Muted,fontSize=13.sp,letterSpacing=2.sp)
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
                    Spacer(Modifier.height(10.dp))
                    Text(if(ui.example) "Built-in example · no live model used" else if(ui.incomplete) "${ui.busy ?: "Incomplete answer"}" else "ChatGPT · ${ui.models.firstOrNull{it.slug==ui.model}?.displayName ?: "Subscription"}",color=Gold,fontSize=16.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(ui.question,fontFamily=Outfit,fontSize=25.sp,fontWeight=FontWeight.SemiBold,maxLines=2)
                    Spacer(Modifier.height(15.dp))
                    val scroll=rememberScrollState()
                    val scope=rememberCoroutineScope()
                    var scrollFocused by remember { mutableStateOf(false) }
                    LaunchedEffect(ui.question) { scroll.scrollTo(0) }
                    Box(Modifier.weight(1f).fillMaxWidth(.82f).clip(RoundedCornerShape(14.dp))
                        .background(Panel).border(if(scrollFocused && !typing) 2.dp else 1.dp,if(scrollFocused && !typing) Gold else Ink.copy(alpha=.12f),RoundedCornerShape(14.dp))
                        .testTag("answer-scroll").focusRequester(requester("answer-scroll")).onFocusChanged { if(it.isFocused) model.focused("answer-scroll") }.focusProperties { up=requester("configuration"); down=requester("follow-up") }
                        .onFocusChanged { scrollFocused=it.isFocused }.onPreviewKeyEvent { event ->
                            if(event.type==KeyEventType.KeyDown && event.key==Key.DirectionDown && scroll.value<scroll.maxValue) { scope.launch{scroll.scrollTo((scroll.value+100).coerceAtMost(scroll.maxValue))};true }
                            else if(event.type==KeyEventType.KeyDown && event.key==Key.DirectionUp && scroll.value>0) { scope.launch{scroll.scrollTo((scroll.value-100).coerceAtLeast(0))};true } else false
                        }.focusable().drawWithContent {
                            drawContent()
                            if(scroll.maxValue>0) {
                                val inset=22.dp.toPx()
                                val fade=28.dp.toPx().coerceAtMost((size.height-2*inset).coerceAtLeast(0f))
                                if(scroll.value>0) {
                                    drawRect(Brush.verticalGradient(listOf(Panel,Color.Transparent),
                                        startY=inset,endY=inset+fade),
                                        topLeft=Offset(inset,inset),size=Size(size.width-2*inset,fade))
                                }
                                if(scroll.value<scroll.maxValue) {
                                    drawRect(Brush.verticalGradient(listOf(Color.Transparent,Panel),
                                        startY=size.height-inset-fade,endY=size.height-inset),
                                        topLeft=Offset(inset,size.height-inset-fade),size=Size(size.width-2*inset,fade))
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
                        Column(Modifier.fillMaxSize().padding(22.dp).clip(RoundedCornerShape(4.dp)).verticalScroll(scroll)) {
                            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                ui.answer.ifBlank { "Waiting for an answer…" }.split("\n\n").forEach { paragraph ->
                                    Text(paragraph,fontFamily=Outfit,fontSize=23.sp,lineHeight=32.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.padding(end=60.dp)) {
                        ui.stopLabel?.let { label -> TvButton(label,Modifier.onFocusChanged { if(it.isFocused) model.focused("answer-stop") }.focusProperties { up=requester("answer-scroll") },onClick=model::stop) }
                        TvButton("Read aloud",Modifier.onFocusChanged { if(it.isFocused) model.focused("read-aloud") }.focusProperties { up=requester("answer-scroll") },enabled=ui.answer.isNotBlank()&&!ui.incomplete,onClick=model::repeatAnswer)
                        TvButton("Follow-up",Modifier.testTag("follow-up").focusRequester(requester("follow-up")).onFocusChanged { if(it.isFocused) model.focused("follow-up") }.focusProperties { up=requester("answer-scroll") },onClick={model.stop();focusManager.clearFocus(force=true);typing=true})
                        TvButton("New conversation",Modifier.testTag("new-conversation").focusRequester(requester("new-conversation")).onFocusChanged { if(it.isFocused) model.focused("new-conversation") }.focusProperties { up=requester("answer-scroll") },onClick=model::newConversation)
                    }
                    Spacer(Modifier.height(10.dp))
                    Status(ui)
                }
                Page.SIGN_IN -> PhoneSignInPanel(ui,onPhoneSignIn,{model.signIn(SignInRoute.MAC,it)},model::cancelSignIn,model::leaveSignIn,model::signInDone,model::enablePlan,model::focused)
                Page.MODELS -> {
                    Text("Choose a model",fontSize=32.sp,modifier=Modifier.padding(top=20.dp,bottom=12.dp))
                    Column(Modifier.weight(1f).widthIn(max=680.dp).verticalScroll(rememberScrollState()).padding(horizontal=10.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        if(ui.models.isEmpty()) Text(ui.message.ifBlank { ui.busy ?: "No models are available. Refresh to try again." },color=Muted,fontSize=20.sp)
                        ui.models.forEach { entry ->
                            val tag="model-option-${entry.slug}"
                            val optionFocus=remember(entry.slug) { FocusRequester() }
                            LaunchedEffect(ui.focusVersion,ui.models) { if(ui.focus==tag) optionFocus.requestFocus() }
                            TvButton((if(entry.slug==ui.model) "✓  " else "")+entry.displayName,Modifier.testTag(tag).focusRequester(optionFocus).onFocusChanged { if(it.isFocused) model.focused(tag) },onClick={model.chooseModel(entry.slug)})
                        }
                        TvButton("Refresh models",Modifier.testTag("models-refresh").focusRequester(requester("models-refresh")),onClick=model::refreshModels)
                        TvButton("Back",onClick={model.back()})
                    }
                    Status(ui)
                }
                Page.SETTINGS -> {
                    Spacer(Modifier.height(18.dp))
                    Text("Configuration",fontFamily=Outfit,fontSize=32.sp,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.height(14.dp))
                    Column(Modifier.weight(1f).widthIn(max=800.dp).verticalScroll(rememberScrollState()).padding(horizontal=10.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement=Arrangement.spacedBy(20.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(when { ui.planAllowed -> "ChatGPT subscription connected"; ui.signedIn -> "ChatGPT plan permission needed"; else -> "Sign in to use your ChatGPT subscription" },fontSize=20.sp)
                            if(ui.signedIn) Text(ui.account,color=Muted,fontSize=17.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        if(!ui.planAllowed) TvButton(if(ui.signedIn) "Allow plan access" else "Continue with ChatGPT",
                            Modifier.testTag("sign-in").focusRequester(requester("sign-in")).onFocusChanged { if(it.isFocused) model.focused("sign-in") },onClick={model.chooseSignInRoute(ui.signedIn)})
                        if(ui.planAllowed) TvButton("Model · "+(ui.models.firstOrNull { it.slug==ui.model }?.displayName ?: "Choose a model"),
                            Modifier.testTag("change-model").focusRequester(requester("change-model")).onFocusChanged { if(it.isFocused) model.focused("change-model") },onClick=model::modelChooser)
                        TvButton("Read answers aloud · "+if(ui.speech) "On" else "Off",onClick=model::toggleSpeech)
                        if(ui.planAllowed) TvButton("Reconnect account",Modifier.testTag("reconnect").focusRequester(requester("reconnect")).onFocusChanged { if(it.isFocused) model.focused("reconnect") },onClick={model.chooseSignInRoute(false)})
                        if(ui.signedIn) TvButton("Sign out",onClick=model::signOut)
                        TvButton("Back",Modifier.testTag("settings-back"),onClick={model.back()})
                    }
                    Spacer(Modifier.height(8.dp))
                    Status(ui)
                }
            }
        }
    }
    }
    if(typing) {
        var text by rememberSaveable { mutableStateOf("") }
        val inputFocus=remember { FocusRequester() }
        val submitFocus=remember { FocusRequester() }
        val cancelFocus=remember { FocusRequester() }
        var inputFocused by remember { mutableStateOf(false) }
        Dialog(onDismissRequest={typing=false;model.dialogClosed()},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
            val keyboardVisible=WindowInsets.ime.getBottom(LocalDensity.current)>0
            Box(Modifier.fillMaxSize().imePadding().padding(24.dp),contentAlignment=Alignment.TopCenter) {
            Column(Modifier.widthIn(max=640.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel).border(1.dp,Ink.copy(alpha=.18f),RoundedCornerShape(22.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(if(ui.page==Page.ANSWER) "Ask a follow-up" else "Type a question",fontFamily=Outfit,fontSize=26.sp,color=Ink)
                BasicTextField(text,{if(it.length<=4000)text=it},Modifier.fillMaxWidth().height(52.dp).background(Background,RoundedCornerShape(12.dp)).border(if(inputFocused) 2.dp else 1.dp,if(inputFocused) Gold else Muted.copy(alpha=.3f),RoundedCornerShape(12.dp)).onFocusChanged { inputFocused=it.isFocused }.padding(horizontal=14.dp,vertical=11.dp).testTag("question-input").focusRequester(inputFocus).focusProperties { down=if(text.isNotBlank()) submitFocus else cancelFocus }
                    .onPreviewKeyEvent { event ->
                        // The editor consumes Down for caret movement before focus search runs.
                        // After keyboard dismissal, Down follows the visible action-row hint.
                        if(!keyboardVisible && event.type==KeyEventType.KeyDown && event.key==Key.DirectionDown) {
                            (if(text.isNotBlank()) submitFocus else cancelFocus).requestFocus()
                            true
                        } else false
                    },
                    decorationBox={ inner -> Box { if(text.isEmpty()) Text("Select to enter your question",color=Muted,fontSize=21.sp); inner() } },
                    singleLine=true,textStyle=TextStyle(color=Ink,fontFamily=Outfit,fontSize=22.sp),cursorBrush=androidx.compose.ui.graphics.SolidColor(Gold))
                Text(if(keyboardVisible) "Back  Close keyboard" else if(inputFocused) "OK  Edit · Down  ${if(text.isNotBlank()) "Ask" else "Cancel"} · Back  Cancel" else "OK  Select · Up  Edit · Back  Cancel",color=Muted,fontSize=15.sp)
                Text("${text.length} / 4,000",color=Muted,fontSize=14.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    TvButton("Ask",Modifier.testTag("question-submit").focusRequester(submitFocus).focusProperties { up=inputFocus },primary=true,enabled=text.isNotBlank(),onClick={typing=false;model.submit(text)})
                    TvButton("Cancel",Modifier.focusRequester(cancelFocus).focusProperties { up=inputFocus },onClick={typing=false;model.dialogClosed()})
                }
            }
            }
        }
        LaunchedEffect(Unit) { inputFocus.requestFocus() }
    }
}
@Composable private fun Status(ui:AskUi) {
    Text(ui.message.ifBlank { ui.busy ?: when(ui.page) { Page.READY -> "OK  Select · Back  Exit"; Page.ANSWER -> if(ui.focus=="answer-scroll") "Up / Down  Scroll · Back  Return" else if(ui.focus=="configuration") "OK  Configuration · Down  Answer · Back  Return" else "OK  Select · Up  Answer · Back  Return"; Page.SETTINGS,Page.MODELS -> "Up / Down  Controls · Back  Return"; Page.SIGN_IN -> "Back  Return" } },
        modifier=Modifier.fillMaxWidth().padding(end=120.dp).heightIn(min=30.dp),color=if(ui.message.isNotBlank()) Gold else Muted,fontSize=15.sp,lineHeight=21.sp,maxLines=3)
}
