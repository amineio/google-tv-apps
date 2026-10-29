package com.amine.asktv

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Only the ADB tunnel exposes this Android loopback listener to the Mac browser. */
class LoopbackAuthServer(private val protocol: OAuthProtocol, private val onGrant: (AuthGrant) -> Unit,
    private val onFailure: (String) -> Unit) : AutoCloseable {
    private val closed=AtomicBoolean(false)
    private val workers=Executors.newFixedThreadPool(2)
    private var server: ServerSocket?=null
    fun start() {
        val socket=ServerSocket()
        try { socket.reuseAddress=true; socket.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"),1455)); socket.soTimeout=1000 }
        catch(e:Exception) { socket.close(); throw e }
        server=socket
        val deadline=System.nanoTime()+600_000_000_000L
        workers.execute {
            try {
                while(!closed.get() && System.nanoTime()<deadline) {
                    try { val client=socket.accept(); workers.execute { handle(client) } } catch (_:SocketTimeoutException) { }
                }
                if(!closed.get()) { protocol.cancel(); onFailure("Sign-in expired. Continue with ChatGPT to try again."); close() }
            } catch (_:Exception) { if(!closed.get()) { protocol.cancel(); onFailure("Sign-in stopped. Please try again."); close() } }
        }
    }
    private fun handle(client:Socket) {
        client.use { s ->
            try {
                s.soTimeout=5000
                val reader=s.getInputStream().bufferedReader(Charsets.US_ASCII)
                val request=reader.readLine()?.takeIf { it.length <= 8192 } ?: return
                var count=0
                while(true) { val line=reader.readLine() ?: return; count+=line.length; if(count>16384) return; if(line.isEmpty()) break }
                val parts=request.split(' ')
                if(parts.size!=3 || parts[0]!="GET") { respond(s,405,"Method not allowed."); return }
                val uri=URI(parts[1])
                when(uri.path) {
                    "/start" -> {
                        if(closed.get()) respond(s,410,"Sign-in ended.")
                        else respond(s,302,"Continue in the browser.",protocol.authorizationUrl())
                    }
                    "/status" -> respond(s,200,if(closed.get()) "ended" else "pending")
                    "/auth/callback" -> {
                        val params=linkedMapOf<String,String>()
                        uri.rawQuery.orEmpty().split('&').filter { it.isNotBlank() }.forEach {
                            val pair=it.split('=',limit=2)
                            val key=URLDecoder.decode(pair[0],"UTF-8")
                            if(params.containsKey(key)) throw AuthFailure()
                            params[key]=URLDecoder.decode(pair.getOrElse(1){""},"UTF-8")
                        }
                        val grant=protocol.callback(params)
                        respond(s,200,"Ask TV received your authorization. Check the TV to confirm sign-in. You can close this tab.")
                        onGrant(grant)
                    }
                    else -> respond(s,404,"Not found.")
                }
            } catch(e:AuthFailure) { runCatching { respond(s,400,"Sign-in could not be completed. Check Ask TV.") }; protocol.cancel(); onFailure(e.safeMessage); close() }
            catch (_:Exception) { runCatching { respond(s,400,"This request could not be read.") } }
        }
    }
    private fun respond(socket:Socket,status:Int,text:String,location:String?=null) {
        val bytes=text.toByteArray(Charsets.UTF_8)
        val header="HTTP/1.1 $status ${if(status==302) "Found" else "OK"}\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nCache-Control: no-store\r\nConnection: close\r\n"+
            (location?.let { "Location: $it\r\n" } ?: "")+"\r\n"
        socket.getOutputStream().apply { write(header.toByteArray(Charsets.US_ASCII)); write(bytes); flush() }
    }
    override fun close() { if(closed.compareAndSet(false,true)) { runCatching { server?.close() }; workers.shutdownNow() } }
}
