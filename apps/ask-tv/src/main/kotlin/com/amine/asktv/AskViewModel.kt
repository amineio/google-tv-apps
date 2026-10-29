package com.amine.asktv

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.concurrent.Executors

enum class Page { READY, ANSWER, SETTINGS }
data class AskUi(val page:Page=Page.READY,val question:String="",val answer:String="",val example:Boolean=false,
    val incomplete:Boolean=false,val busy:String?=null,val message:String="",val signedIn:Boolean=false,
    val planAllowed:Boolean=false,val account:String="Signed out",val models:List<Model> = emptyList(),
    val model:String?=null,val speech:Boolean=true,val focus:String="ask",val focusVersion:Long=0) {
    val stopLabel: String? get() = when(busy) {
        null -> null
        "Speaking…" -> "Stop reading"
        "Listening…" -> "Stop listening"
        else -> "Stop"
    }
    fun afterCancellation(): AskUi = copy(busy=null,message="Stopped.")
}

class AskViewModel(private val app:Application):ViewModel() {
    private val prefs=app.getSharedPreferences("preferences",0)
    private val main=Handler(Looper.getMainLooper())
    private val executor=Executors.newCachedThreadPool()
    private val operation=AskController()
    private val session=ConversationSession()
    private val speech=SpeechController(app)
    private val oauth=OAuthClient()
    private val client=SubscriptionClient()
    private val store=runCatching { SessionStore(EncryptedRecords(app)) }.getOrNull()
    private val accountCompletion=AccountCompletion(operation, { action -> main.post { action() } },
        { store?.snapshot() }, { record -> accountStatus(record) })
    private var returnPage=Page.READY
    private var returnFocus="configuration"
        private var permissionToken:Long?=null
    var ui by mutableStateOf(AskUi(speech=prefs.getBoolean("speech",true),model=prefs.getString("model",null)))
        private set
    init { accountStatus(); if(store==null) ui=ui.copy(message="Secure session storage is unavailable. Reopen Ask TV to try again.") }
    private fun accountStatus(record:CredentialRecord?=store?.snapshot()) {
        ui=ui.copy(signedIn=record?.accessToken!=null,planAllowed=record?.canInfer==true,
            account=if(record?.accessToken!=null) record.email ?: "ChatGPT account connected" else "Signed out")
    }
    private fun focus(target:String) { ui=ui.copy(focus=target,focusVersion=ui.focusVersion+1) }
    fun settings() { stop(); returnPage=ui.page; returnFocus="configuration"; ui=ui.copy(page=Page.SETTINGS,message=""); focus("sign-in"); if(ui.planAllowed) refreshModels() }
    fun back():Boolean {
        if(operation.back()) { permissionToken=null; ui=ui.afterCancellation(); return true }
        if(ui.page==Page.SETTINGS) { ui=ui.copy(page=returnPage,message=""); focus(returnFocus); return true }
        if(ui.page==Page.ANSWER) { ui=ui.copy(page=Page.READY); focus("ask"); return true }
        return false
    }
    fun stop() { operation.stop(); permissionToken=null; speech.stop(); ui=ui.copy(busy=null) }
    fun stopped() { stop(); focus(ui.focus) }
    fun newConversation() { stop(); session.clear(); ui=ui.copy(page=Page.READY,question="",answer="",example=false,incomplete=false,message=""); focus("ask") }
    fun dialogClosed() { focus(if(ui.page==Page.ANSWER) "follow-up" else "type") }
    fun focused(target:String) { ui=ui.copy(focus=target) }
    fun example(index:Int) {
        stop()
        val example=EXAMPLES[index]
        ui=ui.copy(page=Page.ANSWER,question=example.first,answer=example.second,example=true,incomplete=false,message="")
        focus("answer-scroll")
    }
    /** Returns true only when this explicit Ask action needs a permission prompt. */
    fun ask(hasPermission:Boolean):Boolean {
        stop()
        if(!ui.planAllowed) { settings(); ui=ui.copy(message="Continue with ChatGPT to ask a live question. Examples work without sign-in."); return false }
        val token=operation.begin { speech.stop() }
        permissionToken=token
        ui=ui.copy(busy=if(hasPermission) "Listening…" else "Microphone permission",message="")
        if(hasPermission) recognition(token)
        return !hasPermission
    }
    fun permissionResult(allowed:Boolean) {
        val token=permissionToken ?: return
        permissionToken=null
        if(!operation.isCurrent(token)) return
        if(allowed) recognition(token) else { operation.finish(token); ui=ui.copy(busy=null,message=AskController.recognitionError("permission")); focus("type") }
    }
    private fun recognition(token:Long) {
        ui=ui.copy(busy="Listening…")
        speech.recognize({ question ->
            if(operation.isCurrent(token)) { operation.finish(token); submit(question) }
        },{ reason -> if(operation.isCurrent(token)) { operation.finish(token); ui=ui.copy(busy=null,message=AskController.recognitionError(reason)); focus("type") } })
    }
    fun submit(raw:String) {
        val question=raw.trim()
        if(question.isEmpty() || question.length>4000) { ui=ui.copy(message="Enter a question with 4,000 characters or fewer."); return }
        stop()
        if(!ui.planAllowed) { settings(); ui=ui.copy(message="Continue with ChatGPT to ask your question. No question was sent."); return }
        val input=session.input(question)
        val cancel=Cancellation()
        val token=operation.begin { cancel.cancel(); speech.stop() }
        ui=ui.copy(page=Page.ANSWER,question=question,answer="",example=false,incomplete=true,busy="Connecting…",message="")
        focus("answer-scroll")
        executor.execute {
            try {
                val access=access(cancel)
                val models=client.models(access,cancel)
                if(models.isEmpty()) throw AuthFailure("No subscription models are available. Check ChatGPT plan access.")
                val selected=ui.model?.takeIf { saved -> models.any { it.slug==saved } } ?: models.first().slug
                post(token) { ui=ui.copy(models=models,model=selected,busy="Answering…"); prefs.edit().putString("model",selected).apply() }
                val result=client.answer(access,selected,models,input,cancel) { text -> post(token) { ui=ui.copy(answer=text) } }
                post(token) {
                    operation.finish(token)
                    ui=ui.copy(answer=result.text,incomplete=!result.complete,busy=null,message=result.message)
                    if(result.complete) { session.complete(question,result.text); if(ui.speech) repeatAnswer() }
                }
            } catch(e:Exception) { failure(token,e) }
        }
    }
    private fun access(cancel:Cancellation):String = (store ?: throw AuthFailure("Secure session storage is unavailable.")).access(System.currentTimeMillis()/1000) { oauth.refresh(it,cancel) }
    fun refreshModels() {
        if(!ui.planAllowed) return
        stop()
        val cancel=Cancellation(); val token=operation.begin { cancel.cancel() }
        ui=ui.copy(busy="Loading models…",models=emptyList())
        executor.execute { try {
            val models=client.models(access(cancel),cancel)
            post(token) {
                operation.finish(token)
                val selected=ui.model?.takeIf { saved -> models.any { it.slug==saved } } ?: models.firstOrNull()?.slug
                ui=ui.copy(models=models,model=selected,busy=null,message=if(models.isEmpty()) "No subscription models are available." else "")
                prefs.edit().putString("model",selected).apply()
            }
        } catch(e:Exception) { failure(token,e) } }
    }
    fun chooseModel(slug:String) { SubscriptionClient.requireModel(slug,ui.models); ui=ui.copy(model=slug); prefs.edit().putString("model",slug).apply() }
    fun toggleSpeech() { speech.stop(); ui=ui.copy(speech=!ui.speech); prefs.edit().putBoolean("speech",ui.speech).apply() }
    fun repeatAnswer() {
        if(ui.answer.isBlank() || ui.incomplete) return
        stop()
        val token=operation.begin { speech.stop() }
        ui=ui.copy(busy="Speaking…",message="")
        speech.speak(ui.answer) { success ->
            if(operation.isCurrent(token)) { operation.finish(token); ui=ui.copy(busy=null,message=if(success) "" else "Speech is unavailable. The answer remains on screen. Check Android speech settings.") }
        }
    }
    fun signIn() = beginSignIn(requestConsent=false)
    fun enablePlan() = beginSignIn(requestConsent=true)
    private fun beginSignIn(requestConsent:Boolean) {
        stop()
        val record=store?.snapshot() ?: run { ui=ui.copy(message="Secure session storage is unavailable."); return }
        val protocol=OAuthProtocol(record.hostId,record.clientId ?: record.pendingClientId,requestConsent=requestConsent)
        protocol.begin()
        val cancel=Cancellation()
        var server:LoopbackAuthServer?=null
        val token=operation.begin { cancel.cancel(); protocol.cancel(); server?.close() }
        ui=ui.copy(page=Page.SETTINGS,busy="Waiting for browser sign-in…",message="On your Mac, run tools/sign-in-ask-tv.sh with TV_SERIAL. Keep Ask TV open. Sign-in expires after 10 minutes.")
        server=LoopbackAuthServer(protocol,{ grant -> executor.execute {
            try {
                protocol.withActiveAttempt(grant.attempt) {
                    cancel.check()
                    store.rememberIssuedClient(grant.clientId)
                }
                val next=oauth.exchange(grant,store.snapshot(),cancel)
                cancel.check()
                protocol.commit(grant.attempt) { cancel.check(); store.install(next) }
                server?.close()
                accountCompletion.committed(token) {
                    operation.finish(token); ui=ui.copy(busy=null,message=if(next.canInfer) "Connected through your ChatGPT subscription." else "Signed in. ChatGPT plan use was not granted. Choose Enable ChatGPT plan to allow subscription access.")
                    if(next.canInfer) refreshModels()
                }
            } catch(e:Exception) { protocol.cancel(); server?.close(); failure(token,e) }
        } },{ message -> post(token) { operation.finish(token); ui=ui.copy(busy=null,message=message) } })
        try { server.start() } catch (_:Exception) { operation.stop(); ui=ui.copy(busy=null,message="The sign-in listener could not start. Close another sign-in attempt and try again.") }
    }
    fun signOut() {
        stop()
        val store=store ?: return
        val cancel=Cancellation(); val token=operation.begin { cancel.cancel() }
        ui=ui.copy(busy="Signing out…")
        executor.execute {
            try {
                val confirmed=store.signOut { oauth.revoke(it,cancel) }
                main.post {
                    // Account state follows the current durable record, even after operation cancellation.
                    accountStatus()
                    if(operation.isCurrent(token)) {
                        operation.finish(token)
                        session.clear()
                        ui=ui.copy(models=emptyList(),busy=null,answer="",question="",message=if(confirmed) "Signed out." else "Signed out locally. Remote revocation was not confirmed. You can disconnect Ask TV in ChatGPT Settings.")
                    }
                }
            } catch(e:Exception) { failure(token,e) }
        }
    }
    private fun post(token:Long,action:()->Unit) { main.post { if(operation.isCurrent(token)) action() } }
    private fun failure(token:Long,error:Exception) { post(token) {
        operation.finish(token); accountStatus()
        ui=ui.copy(busy=null,message=when(error) { is AuthFailure -> error.safeMessage; is ApiFailure -> error.message.orEmpty(); else -> "The request could not finish. Check your connection and try again." })
    } }
    override fun onCleared() { operation.stop(); speech.close(); executor.shutdownNow() }
}

val EXAMPLES=listOf(
    "Why is the sky blue?" to "Sunlight looks white, but it contains many colors. As sunlight enters Earth's atmosphere, tiny gas molecules scatter it in all directions.\n\nBlue light has a shorter wavelength than red light, so the air scatters blue light more strongly. Some of that scattered blue light reaches your eyes from every part of the sky.\n\nAt sunrise and sunset, sunlight travels through more air. Much of the blue light scatters away before it reaches you. More red and orange light remains, so the horizon can look warm.\n\nClouds look different because their larger water droplets scatter the visible colors more evenly. Together, those colors look white.\n\nThis is a built-in example preview. It does not use a model or a connected account.",
    "Give me a simple dinner idea" to "Try a quick tomato and chickpea skillet.\n\nWarm a little olive oil in a pan. Add chopped onion and cook until soft. Add a drained can of chickpeas and a can of chopped tomatoes. Season with a small pinch of salt and your favorite herbs.\n\nLet it simmer for about ten minutes, stirring now and then. Add a handful of spinach near the end. Serve with bread, rice, or couscous.\n\nFor a different version, add lemon juice and a spoonful of plain yogurt at the table. Use ingredients that suit your diet and any food allergies.\n\nThis is a built-in example preview. It does not use a model or a connected account."
)
