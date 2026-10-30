package com.amine.asktv

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MacSignInReplacementTest {
    private class ExclusiveHost(private val releaseClose:CountDownLatch?=null):MacSignInHost {
        val bindAttempted=CountDownLatch(1)
        val listening=CountDownLatch(1)
        val closeEntered=CountDownLatch(1)
        @Volatile private var socket:ServerSocket?=null
        override fun start() {
            bindAttempted.countDown()
            val listener=ServerSocket()
            try {
                listener.reuseAddress=false
                listener.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"),1455),1)
                socket=listener
                listening.countDown()
            } catch(error:Exception) { listener.close(); throw error }
        }
        override fun close() {
            closeEntered.countDown()
            try { releaseClose?.let { check(it.await(5,TimeUnit.SECONDS)) } }
            finally { socket?.close() }
        }
    }

    @Test fun replacementWaitsForRetiringMacListenerToReleaseExclusivePort() {
        val releaseOldClose=CountDownLatch(1)
        val old=ExclusiveHost(releaseOldClose)
        val replacement=ExclusiveHost()
        // Production uses concurrent workers. A serial test executor would hide this race.
        val fixture=TvFlowFixture(worker=Executors.newCachedThreadPool())
        val created=AtomicInteger()
        fixture.macHostFactory={ if(created.getAndIncrement()==0) old else replacement }
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        try {
            instrumentation.runOnMainSync { fixture.model.signIn() }
            assertTrue("Original listener bound loopback 1455",old.listening.await(3,TimeUnit.SECONDS))
            instrumentation.runOnMainSync { fixture.model.signIn() }
            assertTrue("Old cleanup reached the delayed close",old.closeEntered.await(3,TimeUnit.SECONDS))
            assertFalse("Replacement attempted bind while the old listener still owned port 1455",
                replacement.bindAttempted.await(250,TimeUnit.MILLISECONDS))
            releaseOldClose.countDown()
            assertTrue("Replacement bound after the old port was released",replacement.listening.await(3,TimeUnit.SECONDS))
        } finally {
            releaseOldClose.countDown()
            fixture.close()
        }
    }
}
