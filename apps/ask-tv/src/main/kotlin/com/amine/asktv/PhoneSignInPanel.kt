package com.amine.asktv

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay

internal const val PHONE_SETUP_GUIDE="https://github.com/amineio/google-tv-apps/blob/main/apps/ask-tv/README.md#install-the-phone-helper-once"

internal fun phoneQrBitmap(link:String):Bitmap {
    val matrix=QRCodeWriter().encode(link,BarcodeFormat.QR_CODE,512,512,mapOf(EncodeHintType.MARGIN to 4))
    val pixels=IntArray(512*512) { index -> if(matrix[index%512,index/512]) 0xFF0D0E12.toInt() else android.graphics.Color.WHITE }
    return Bitmap.createBitmap(pixels,512,512,Bitmap.Config.ARGB_8888)
}

@Composable internal fun PhoneSignInPanel(ui:AskUi,onPhone:(Boolean)->Unit,onMac:(Boolean)->Unit,
    onCancel:()->Unit,onBack:()->Unit,onDone:()->Unit,onEnablePlan:()->Unit,onFocused:(String)->Unit) {
    val names=listOf("sign-in-phone-route","sign-in-mac-route","sign-in-back","phone-sign-in-cancel","phone-sign-in-mac",
        "phone-sign-in-retry","sign-in-retry","mac-sign-in-cancel","phone-sign-in-done","phone-sign-in-enable-plan","phone-setup","phone-setup-back")
    val focus=remember { names.associateWith { FocusRequester() } }
    var setup by rememberSaveable { mutableStateOf(false) }
    fun control(tag:String)=Modifier.testTag(tag).focusRequester(focus.getValue(tag)).onFocusChanged { if(it.isFocused) onFocused(tag) }
    LaunchedEffect(ui.focusVersion,ui.signInState,ui.signInRoute,setup) {
        focus[if(setup) "phone-setup-back" else ui.focus]?.let { runCatching { it.requestFocus() } }
    }
    fun closeSetup() { onFocused("phone-setup"); setup=false }
    BackHandler(setup) { closeSetup() }
    val phone=ui.signInRoute==SignInRoute.PHONE
    val waiting=ui.signInState in setOf("waiting","verifying","permission","starting")
    val committed=ui.signInState in setOf("connected","plan_required")
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(ui.phonePairing?.expiresAtElapsedMs) { while(true) { now=SystemClock.elapsedRealtime(); delay(1000) } }
    Column(Modifier.fillMaxSize().padding(top=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text(when {
            setup -> "Set up the phone helper"
            ui.signInState=="choose" -> if(ui.requestConsent) "Allow ChatGPT plan access" else "Continue with ChatGPT"
            ui.signInState=="connected" -> "Your TV is connected"
            ui.signInState=="plan_required" -> "Allow ChatGPT plan access"
            ui.signInState=="canceled" -> "Sign-in canceled"
            ui.signInState=="expired" -> "Sign-in expired"
            ui.signInState=="failed" -> "Sign-in did not finish"
            phone -> "Sign in with your phone"
            else -> "Sign in using your Mac"
        },fontSize=32.sp,fontWeight=FontWeight.SemiBold)
        when {
            setup -> {
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(28.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                        val guide=remember { phoneQrBitmap(PHONE_SETUP_GUIDE).asImageBitmap() }
                        Image(guide,"Phone setup guide QR",Modifier.size(220.dp).background(Color.White).testTag("phone-setup-guide-qr"))
                        Text("SETUP GUIDE · opens a web page",color=Gold,fontSize=14.sp)
                    }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text("Install once from your Mac",fontSize=23.sp)
                        Text("Use an Android 14+ phone. Connect it to your Mac and allow USB debugging.",color=Muted,fontSize=18.sp)
                        Text("Install AI Chat privately with ADB from this repository on your Mac. The guide has the build and install commands.",color=Muted,fontSize=18.sp)
                        Text("Scan this guide with your phone camera. It is not a pairing code or an APK download.",color=Muted,fontSize=18.sp)
                        Text("After installation, open AI Chat on the phone. Return here and choose Use Android phone.",color=Muted,fontSize=18.sp)
                    }
                }
                TvButton("Back to sign-in choices",control("phone-setup-back"),onClick=::closeSetup)
            }
            ui.signInState=="choose" -> {
                Text(if(ui.requestConsent) "Sign in again and allow AI Chat to use your existing ChatGPT plan." else "Choose where to open the secure ChatGPT sign-in page.",color=Muted,fontSize=19.sp)
                Text("Phone: install the helper once from your Mac. Already installed? Open AI Chat on your phone.",color=Muted,fontSize=18.sp,modifier=Modifier.widthIn(max=710.dp))
                Text("Mac helper: requires this repository and an ADB connection to your TV.",color=Muted,fontSize=18.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    TvButton("Use Android phone",control("sign-in-phone-route").focusProperties { down=focus.getValue("phone-setup") },onClick={onPhone(ui.requestConsent)})
                    TvButton("Use Mac helper",control("sign-in-mac-route"),onClick={onMac(ui.requestConsent)})
                    TvButton("Back",control("sign-in-back"),onClick=onBack)
                }
                TvButton("Phone setup guide · install once",control("phone-setup").focusProperties { up=focus.getValue("sign-in-phone-route") },onClick={setup=true})
                Text("The pairing QR does not install the helper.",color=Muted,fontSize=16.sp)
            }
            waiting && phone -> {
                Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(32.dp)) {
                    val link=ui.phonePairing?.link
                    if(link!=null) {
                        val bitmap=remember(link) { phoneQrBitmap(link).asImageBitmap() }
                        val qrSize=with(LocalDensity.current) { 512.toDp() }
                        Image(bitmap,"Phone pairing QR code",Modifier.requiredSize(qrSize).background(Color.White).testTag("phone-sign-in-qr"))
                    }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text(when {
                            ui.signInState=="permission" -> "Allow local network access on this TV."
                            ui.signInState=="verifying" -> "Checking your sign-in…"
                            ui.phonePairing!=null && link==null -> "Phone connected"
                            else -> "Already installed? Open AI Chat on your phone."
                        },fontSize=23.sp)
                        if(ui.phonePairing!=null && link==null) {
                            Text(if(ui.signInState=="verifying") "Your phone sent authorization. Wait while the TV checks your account." else "Finish ChatGPT sign-in in your phone browser. Keep this TV screen open.",color=Muted,fontSize=21.sp,lineHeight=28.sp)
                        } else {
                            Text("1  Connect the phone and TV to the same Wi-Fi.",color=Muted,fontSize=18.sp)
                            Text("2  Choose Photograph TV code in AI Chat, then Continue with ChatGPT.",color=Muted,fontSize=18.sp,lineHeight=25.sp)
                            Text("Pairing code only · Install the helper first using Phone setup guide.",Modifier.testTag("phone-helper-install-instructions"),color=Muted,fontSize=16.sp)
                        }
                        if(ui.phonePairing!=null) {
                            val seconds=((ui.phonePairing.expiresAtElapsedMs-now).coerceAtLeast(0)+999)/1000
                            Text("${if(link==null) "Sign-in" else "Code"} expires in ${seconds/60}:${(seconds%60).toString().padStart(2,'0')}",color=Muted,fontSize=17.sp)
                        }
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    TvButton("Cancel",control("phone-sign-in-cancel"),onClick=onCancel)
                    TvButton("Use Mac helper",control("phone-sign-in-mac"),onClick={onMac(ui.requestConsent)})
                }
            }
            waiting -> {
                Column(Modifier.weight(1f).testTag("mac-sign-in-instructions"),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Text("Finish sign-in in your Mac browser.",fontSize=23.sp)
                    Text("1  Connect this TV to your Mac with the AI Chat ADB helper.",color=Muted,fontSize=19.sp)
                    Text("2  From the app repository, run:",color=Muted,fontSize=19.sp)
                    Text("TV_SERIAL='<connected serial>' tools/sign-in-ask-tv.sh",color=Ink,fontSize=18.sp)
                    Text("3  Continue with ChatGPT in the browser. Keep this screen open.",color=Muted,fontSize=19.sp)
                    Text("This attempt expires after 10 minutes.",color=Muted,fontSize=17.sp)
                }
                TvButton("Cancel",control("mac-sign-in-cancel"),onClick=onCancel)
            }
            committed -> {
                Text(if(ui.signInState=="connected") "Connected through your ChatGPT subscription." else "Your account is saved. Allow plan access to ask live questions.",color=Muted,fontSize=22.sp)
                if(ui.signInState=="plan_required") Text("Next, choose phone or Mac and approve plan access in the browser.",color=Muted,fontSize=19.sp)
                Spacer(Modifier.weight(1f))
                if(ui.signInState=="connected") TvButton("Done",control("phone-sign-in-done"),onClick=onDone)
                else Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    TvButton("Allow plan access",control("phone-sign-in-enable-plan"),onClick=onEnablePlan)
                    TvButton("Back",control("sign-in-back"),onClick=onBack)
                }
            }
            else -> {
                Text(ui.message.ifBlank { if(ui.signInState=="canceled") "You can start again when you are ready." else "Start a new attempt, or use your Mac." },color=Muted,fontSize=21.sp,modifier=Modifier.widthIn(max=720.dp))
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    TvButton("Try again",control(if(phone) "phone-sign-in-retry" else "sign-in-retry"),onClick={if(phone) onPhone(ui.requestConsent) else onMac(ui.requestConsent)})
                    if(phone) TvButton("Use Mac helper",control("phone-sign-in-mac"),onClick={onMac(ui.requestConsent)})
                    TvButton("Back",control("sign-in-back"),onClick=onBack)
                }
            }
        }
        Text(if(waiting) "Back  Cancel sign-in" else "Back  Return",color=Muted,fontSize=15.sp)
    }
}
