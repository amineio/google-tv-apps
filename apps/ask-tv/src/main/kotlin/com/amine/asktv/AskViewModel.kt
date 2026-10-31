package com.amine.asktv

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.concurrent.Executors
import java.util.concurrent.Executor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import com.amine.asktv.phoneauth.RelayState

enum class Page { READY, ANSWER, SETTINGS, SIGN_IN, MODELS }
enum class SignInRoute { PHONE, MAC }
internal interface MacSignInHost { fun start(); fun close() }
internal data class AskDependencies(
    val store: SessionStore?, val worker: Executor,
    val exchange: (AuthGrant, CredentialRecord, Cancellation) -> CredentialRecord,
    val models: (String, Cancellation) -> List<Model>,
    val revoke: (CredentialRecord, Cancellation) -> Boolean,
    val phoneFactory: ((OAuthProtocol, AuthGrant, Long) -> Unit, (Long, PhonePairingUi) -> Unit) -> PhoneSignInCoordinator,
    val macFactory: (OAuthProtocol, (AuthGrant) -> Unit, (String) -> Unit) -> MacSignInHost,
)
internal class GrantCompletion(private val store: SessionStore,
    private val exchange: (AuthGrant, CredentialRecord, Cancellation) -> CredentialRecord) {
    fun complete(protocol: OAuthProtocol, grant: AuthGrant, cancel: Cancellation, terminal: (RelayState) -> Unit): CredentialRecord =
        completeActive(protocol,grant,cancel,{true},terminal)
    fun completeActive(protocol: OAuthProtocol, grant: AuthGrant, cancel: Cancellation, active:()->Boolean, terminal: (RelayState)->Unit):CredentialRecord {
        protocol.withActiveAttempt(grant.attempt) { if(!active()) throw AuthFailure(); cancel.check(); store.rememberIssuedClient(grant.clientId) }
        val next = exchange(grant, store.snapshot(), cancel)
        cancel.check()
        protocol.commit(grant.attempt) {
            if(!active()) throw AuthFailure()
            cancel.check()
            store.install(next)
            terminal(if (next.canInfer) RelayState.CONNECTED else RelayState.PLAN_REQUIRED)
        }
        return store.snapshot()
    }
    fun cancelAndSignOut(protocol: OAuthProtocol, cancel: Cancellation, revoke: (CredentialRecord) -> Boolean) =
        cancelAndSignOut(listOf(protocol to cancel), revoke)
    fun cancelAndSignOut(attempts: List<Pair<OAuthProtocol, Cancellation>>, revoke: (CredentialRecord) -> Boolean): Boolean {
        attempts.forEach { it.second.cancel() }
        attempts.forEach { it.first.cancel() }
        return store.signOut(revoke)
    }
}
data class AskUi(val page:Page=Page.READY,val question:String="",val answer:String="",val example:Boolean=false,
    val incomplete:Boolean=false,val busy:String?=null,val message:String="",val signedIn:Boolean=false,
    val planAllowed:Boolean=false,val account:String="Signed out",val models:List<Model> = emptyList(),
    val model:String?=null,val speech:Boolean=true,val focus:String="ask",val focusVersion:Long=0,
    val signInRoute:SignInRoute?=null,val signInState:String="choose",val requestConsent:Boolean=false,
    val phonePairing:PhonePairingUi?=null) {
    val stopLabel: String? get() = when(busy) {
        null -> null
        "Speaking…" -> "Stop reading"
        "Listening…" -> "Stop listening"
        else -> "Stop"
    }
    fun afterCancellation(): AskUi = copy(busy=null,message="Stopped.")
}

class AskViewModel private constructor(private val app:Application, private val dependencies:AskDependencies?, @Suppress("UNUSED_PARAMETER") marker:Unit):ViewModel() {
    constructor(app:Application):this(app,null,Unit)
    internal constructor(app:Application, dependencies:AskDependencies):this(app,dependencies,Unit)
    private val prefs=app.getSharedPreferences("preferences",0)
    private val main=Handler(Looper.getMainLooper())
    private val ownedExecutor=if(dependencies==null) Executors.newCachedThreadPool() else null
    private val executor:Executor=dependencies?.worker ?: ownedExecutor!!
    private val operation=AskController()
    private val session=ConversationSession()
    private val speech=SpeechController(app)
    private val oauth=OAuthClient()
    private val client=SubscriptionClient()
    private val store=if(dependencies!=null) dependencies.store else runCatching { SessionStore(EncryptedRecords(app)) }.getOrNull()
    private val grantCompletion=store?.let { GrantCompletion(it, dependencies?.exchange ?: oauth::exchange) }
    private class AuthOwner(val route:SignInRoute,val cancel:Cancellation=Cancellation()) {
        var token=0L
        @Volatile var protocol:OAuthProtocol?=null
        @Volatile var phone:PhoneSignInCoordinator?=null
        @Volatile var mac:MacSignInHost?=null
        val startDone=CompletableFuture<Unit>()
        val released=CompletableFuture<Unit>()
        val retiring=java.util.concurrent.atomic.AtomicBoolean(false)
    }
    private val authOwners=CopyOnWriteArrayList<AuthOwner>()
    private var activeAuth:AuthOwner?=null
    // Presentation outlives cancellation until this specific sign-in surface is replaced.
    private var signInPresentationOwner:AuthOwner?=null
    private var signingOut=false
    private var signInReturnPage=Page.READY
    private var signInReturnFocus="ready-sign-in"
    private var networkPermissionToken:Long?=null
    private val accountCompletion=AccountCompletion(operation, { action -> main.post { action() } },
        { store?.snapshot() }, { record -> accountStatus(record) })
    private var returnPage=Page.READY
    private var returnFocus="configuration"
        private var permissionToken:Long?=null
    var ui by mutableStateOf(AskUi(speech=prefs.getBoolean("speech",true),model=prefs.getString("model",null)))
        private set
    init { accountStatus(); focus(if(ui.planAllowed) "ask" else "ready-sign-in"); if(store==null) ui=ui.copy(message="Secure session storage is unavailable. Reopen AI Chat to try again.") }
    private fun accountStatus(record:CredentialRecord?=store?.snapshot()) {
        ui=ui.copy(signedIn=record?.accessToken!=null,planAllowed=record?.canInfer==true,
            account=if(record?.accessToken!=null) record.email ?: "ChatGPT account connected" else "Signed out")
    }
    private fun focus(target:String) { ui=ui.copy(focus=target,focusVersion=ui.focusVersion+1) }
    fun settings() { stop(); returnPage=ui.page; returnFocus="configuration"; ui=ui.copy(page=Page.SETTINGS,message=""); focus(if(ui.planAllowed) "change-model" else "sign-in"); if(ui.planAllowed) refreshModels() }
    fun back():Boolean {
        if(ui.page==Page.SIGN_IN) {
            if(ui.signInState in setOf("waiting","verifying","permission","starting")) cancelSignIn() else leaveSignIn()
            return true
        }
        if(ui.page==Page.MODELS) { ui=ui.copy(page=Page.SETTINGS); focus("change-model"); return true }
        if(operation.back()) { permissionToken=null; ui=ui.afterCancellation(); return true }
        if(ui.page==Page.SETTINGS) { ui=ui.copy(page=returnPage,message=""); focus(returnFocus); return true }
        if(ui.page==Page.ANSWER) { ui=ui.copy(page=Page.READY); focus(if(ui.planAllowed) "ask" else "ready-sign-in"); return true }
        return false
    }
    fun stop() {
        operation.stop(); permissionToken=null; networkPermissionToken=null
        activeAuth?.let(::retire); activeAuth=null
        speech.stop(); ui=ui.copy(busy=null)
    }
    fun stopped() { if(ui.page==Page.SIGN_IN && ui.signInState!="choose") cancelSignIn() else { stop(); focus(ui.focus) } }
    fun newConversation() { stop(); session.clear(); ui=ui.copy(page=Page.READY,question="",answer="",example=false,incomplete=false,message=""); focus(if(ui.planAllowed) "ask" else "ready-sign-in") }
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
                val models=(dependencies?.models ?: client::models)(access,cancel)
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
            val models=(dependencies?.models ?: client::models)(access(cancel),cancel)
            post(token) {
                operation.finish(token)
                val selected=ui.model?.takeIf { saved -> models.any { it.slug==saved } } ?: models.firstOrNull()?.slug
                ui=ui.copy(models=models,model=selected,busy=null,message=if(models.isEmpty()) "No subscription models are available." else "")
                prefs.edit().putString("model",selected).apply()
                if(ui.page==Page.MODELS && ui.focus=="models-refresh" && selected!=null) focus("model-option-$selected")
            }
        } catch(e:Exception) { failure(token,e) } }
    }
    fun modelChooser() { ui=ui.copy(page=Page.MODELS); focus(ui.models.firstOrNull { it.slug==ui.model }?.let { "model-option-${it.slug}" } ?: ui.models.firstOrNull()?.let { "model-option-${it.slug}" } ?: "models-refresh") }
    fun chooseModel(slug:String) { SubscriptionClient.requireModel(slug,ui.models); ui=ui.copy(model=slug,page=Page.SETTINGS); prefs.edit().putString("model",slug).apply(); focus("change-model") }
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
    fun chooseSignInRoute(requestConsent:Boolean=false) {
        if(signingOut) return
        signInPresentationOwner=null
        if(ui.page!=Page.SIGN_IN) { signInReturnPage=ui.page; signInReturnFocus=ui.focus }
        stop()
        ui=ui.copy(page=Page.SIGN_IN,signInRoute=null,signInState="choose",requestConsent=requestConsent,phonePairing=null,message="")
        focus("sign-in-phone-route")
    }
    fun enablePlan() = chooseSignInRoute(true)
    fun signIn() = signIn(SignInRoute.MAC)
    fun beginPhonePermission(requestConsent:Boolean):Long {
        signInPresentationOwner=null
        stop()
        val token=operation.begin {}
        networkPermissionToken=token
        ui=ui.copy(page=Page.SIGN_IN,signInRoute=SignInRoute.PHONE,signInState="permission",requestConsent=requestConsent,phonePairing=null,message="")
        focus("phone-sign-in-cancel")
        return token
    }
    fun phonePermissionResult(token:Long, allowed:Boolean) {
        if(networkPermissionToken!=token || !operation.isCurrent(token)) return
        networkPermissionToken=null
        if(allowed) signIn(SignInRoute.PHONE,ui.requestConsent) else {
            operation.finish(token)
            ui=ui.copy(signInState="failed",message="Local network permission is off. Allow it to connect your phone, or use Mac.")
            focus("phone-sign-in-retry")
        }
    }
    fun signIn(route:SignInRoute,requestConsent:Boolean=false) {
        if(signingOut) return
        if(ui.page!=Page.SIGN_IN) { signInReturnPage=ui.page; signInReturnFocus=ui.focus }
        stop()
        val record=store?.snapshot() ?: run { ui=ui.copy(message="Secure session storage is unavailable."); return }
        val owner=AuthOwner(route)
        authOwners.removeAll { it.released.isDone }
        val macReleases=authOwners.filter { it.route==SignInRoute.MAC && it.retiring.get() }.map { it.released }
        authOwners.add(owner); activeAuth=owner; signInPresentationOwner=owner
        owner.token=operation.begin { retire(owner) }
        ui=ui.copy(page=Page.SIGN_IN,signInRoute=route,signInState="waiting",requestConsent=requestConsent,phonePairing=null,busy="Waiting for sign-in…",message="")
        focus(if(route==SignInRoute.PHONE) "phone-sign-in-cancel" else "mac-sign-in-cancel")
        if(route==SignInRoute.PHONE) {
            val grant:(OAuthProtocol,AuthGrant,Long)->Unit={ protocol,value,_ ->
                owner.protocol=protocol
                completeGrant(owner,protocol,value)
            }
            val update:(Long,PhonePairingUi)->Unit={ _,state -> main.post {
                if(activeAuth===owner && ui.page==Page.SIGN_IN) {
                    val previous=ui.signInState
                    ui=ui.copy(phonePairing=state,signInState=state.state,busy=if(state.state in setOf("waiting","verifying")) "Waiting for sign-in…" else null)
                    if(previous!=state.state && state.state !in setOf("waiting","verifying")) terminalFocus(state.state)
                }
            } }
            val phone=dependencies?.phoneFactory?.invoke(grant,update) ?: PhoneSignInCoordinator(app,grant,update)
            owner.phone=phone
            phone.start(record,requestConsent,owner.token)
            owner.startDone.complete(Unit)
        } else CompletableFuture.allOf(*macReleases.toTypedArray()).whenComplete { _,_ -> executor.execute {
            try {
                // A replaced attempt may be canceled while an older listener still owns 1455.
                if(owner.retiring.get() || owner.cancel.canceled) return@execute
                val currentRecord=store?.snapshot() ?: record
                val protocol=OAuthProtocol(currentRecord.hostId,currentRecord.clientId ?: currentRecord.pendingClientId,requestConsent)
                owner.protocol=protocol
                protocol.begin()
                val grant:(AuthGrant)->Unit={ completeGrant(owner,protocol,it) }
                val failed:(String)->Unit={ message -> main.post {
                    if(activeAuth===owner) { retire(owner); operation.finish(owner.token); ui=ui.copy(signInState="failed",busy=null,message=message); terminalFocus("failed") }
                } }
                val server=dependencies?.macFactory?.invoke(protocol,grant,failed) ?: run {
                    val delegate=LoopbackAuthServer(protocol,grant,failed)
                    object:MacSignInHost { override fun start()=delegate.start(); override fun close()=delegate.close() }
                }
                owner.mac=server
                if(!owner.retiring.get() && !owner.cancel.canceled) server.start()
            } catch(_:Exception) { main.post { if(activeAuth===owner) { retire(owner); operation.finish(owner.token); ui=ui.copy(signInState="failed",busy=null,message="The Mac sign-in listener could not start. Close another attempt and retry."); terminalFocus("failed") } } }
            finally { owner.startDone.complete(Unit) }
        } }
    }
    private fun completeGrant(owner:AuthOwner, protocol:OAuthProtocol, grant:AuthGrant) {
        executor.execute {
            try {
                val next=grantCompletion!!.completeActive(protocol,grant,owner.cancel,{!owner.retiring.get()}) { owner.phone?.terminal(it) }
                if(owner.phone==null) retire(owner)
                accountCompletion.committed(owner.token) {
                    operation.finish(owner.token)
                }
                // The durable result also wins a Cancel that preceded queued UI delivery.
                // Identity, not operation liveness, protects a newer route/surface here.
                main.post {
                    if(signInPresentationOwner===owner && ui.page==Page.SIGN_IN) {
                        val state=if(next.canInfer) "connected" else "plan_required"
                        ui=ui.copy(busy=null,signInState=state,message="")
                        terminalFocus(state)
                    }
                }
            } catch(e:Exception) {
                owner.phone?.terminal(RelayState.FAILED)
                executor.execute { protocol.cancel() }
                if(owner.phone==null) retire(owner)
                post(owner.token) { operation.finish(owner.token); accountStatus(); ui=ui.copy(signInState="failed",busy=null,message=if(e is AuthFailure) e.safeMessage else "Sign-in could not finish. Try again."); terminalFocus("failed") }
            }
        }
    }
    private fun terminalFocus(state:String) { focus(when(state) {
        "connected" -> "phone-sign-in-done"
        "plan_required" -> "phone-sign-in-enable-plan"
        else -> if(ui.signInRoute==SignInRoute.PHONE) "phone-sign-in-retry" else "sign-in-retry"
    }) }
    private fun retire(owner:AuthOwner) {
        if(!owner.retiring.compareAndSet(false,true)) return
        executor.execute { owner.cancel.cancel() }
        owner.phone?.let { phone ->
            phone.close()
            phone.released.whenComplete { _,_ -> owner.released.complete(Unit) }
        } ?: owner.startDone.whenComplete { _,_ -> executor.execute {
            try { owner.protocol?.cancel(); owner.mac?.close() }
            finally { owner.released.complete(Unit) }
        } }
    }
    fun cancelSignIn() {
        val committed=ui.signInState in setOf("connected","plan_required")
        stop(); accountStatus()
        ui=ui.copy(signInState=if(committed) ui.signInState else "canceled",phonePairing=null,message="")
        terminalFocus(ui.signInState)
    }
    fun leaveSignIn() {
        signInPresentationOwner=null
        stop()
        ui=ui.copy(page=signInReturnPage,phonePairing=null,message="")
        focus(if(ui.page==Page.READY && ui.planAllowed) "ask" else signInReturnFocus)
    }
    fun signInDone() { signInPresentationOwner=null; stop(); ui=ui.copy(page=Page.READY,phonePairing=null,message=""); focus(if(ui.planAllowed) "ask" else "ready-sign-in") }
    fun signOut() {
        if(signingOut) return
        signInPresentationOwner=null
        signingOut=true
        stop()
        val store=store ?: run { signingOut=false; return }
        val retiring=authOwners.toList()
        val cancel=Cancellation(); val token=operation.begin { cancel.cancel() }
        ui=ui.copy(busy="Signing out…")
        // Compose completion instead of blocking a worker needed by queued cleanup.
        CompletableFuture.allOf(*retiring.map { it.released }.toTypedArray()).whenComplete { _,_ -> executor.execute {
            try {
                val confirmed=store.signOut { (dependencies?.revoke ?: oauth::revoke)(it,cancel) }
                main.post {
                    signingOut=false
                    accountStatus()
                    session.clear()
                    if(operation.isCurrent(token)) {
                        operation.finish(token)
                        ui=ui.copy(models=emptyList(),busy=null,answer="",question="",message=if(confirmed) "Signed out." else "Signed out locally. Remote revocation was not confirmed.")
                        focus("sign-in")
                    }
                }
            } catch(e:Exception) { main.post { signingOut=false }; failure(token,e) }
        } }
    }
    private fun post(token:Long,action:()->Unit) { main.post { if(operation.isCurrent(token)) action() } }
    private fun failure(token:Long,error:Exception) { post(token) {
        operation.finish(token); accountStatus()
        ui=ui.copy(busy=null,message=when(error) { is AuthFailure -> error.safeMessage; is ApiFailure -> error.message.orEmpty(); else -> "The request could not finish. Check your connection and try again." })
        if(error is AuthFailure || error is ApiFailure && error.status in setOf(401,403)) {
            ui=ui.copy(message=ui.message+if(ui.signedIn && ui.planAllowed) " Open Configuration and choose Reconnect account." else " Continue with ChatGPT in Configuration to reconnect.")
            if(ui.page==Page.SETTINGS && !ui.planAllowed) focus("sign-in")
        }
    } }
    override fun onCleared() {
        stop(); speech.close()
        // Accepted cleanup must finish; an interrupted executor could strand a listener or commit.
        CompletableFuture.allOf(*authOwners.map { it.released }.toTypedArray()).whenComplete { _,_ -> ownedExecutor?.shutdown() }
    }
}

val EXAMPLES=listOf(
    "Why is the sky blue?" to "Sunlight looks white, but it contains many colors. As sunlight enters Earth's atmosphere, tiny gas molecules scatter it in all directions.\n\nBlue light has a shorter wavelength than red light, so the air scatters blue light more strongly. Some of that scattered blue light reaches your eyes from every part of the sky.\n\nAt sunrise and sunset, sunlight travels through more air. Much of the blue light scatters away before it reaches you. More red and orange light remains, so the horizon can look warm.\n\nClouds look different because their larger water droplets scatter the visible colors more evenly. Together, those colors look white.\n\nThis is a built-in example. No live model was used.",
    "Give me a simple dinner idea" to "Try a quick tomato and chickpea skillet.\n\nWarm a little olive oil in a pan. Add chopped onion and cook until soft. Add a drained can of chickpeas and a can of chopped tomatoes. Season with a small pinch of salt and your favorite herbs.\n\nLet it simmer for about ten minutes, stirring now and then. Add a handful of spinach near the end. Serve with bread, rice, or couscous.\n\nFor a different version, add lemon juice and a spoonful of plain yogurt at the table. Use ingredients that suit your diet and any food allergies.\n\nThis is a built-in example. No live model was used."
)
