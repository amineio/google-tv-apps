package com.amine.asktv
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

fun record(subject:String="subject",access:String="old",expires:Long=0,scopes:Set<String> = REQUIRED_SCOPES)=CredentialRecord(hostId="urn:uuid:test",clientId="issued",subject=subject,issuer="https://auth.openai.com",email="viewer@example.test",accessToken=access,refreshToken="refresh-$access",idToken="id-$access",expiresAt=expires,scopes=scopes)
class MemoryRecords(var value:CredentialRecord=record()): RecordStorage { var writes=0; var fail=false; override fun read()=value; override fun write(record:CredentialRecord) { if(fail) throw java.io.IOException(); value=record; writes++ } }
class SessionStoreTest {
    @Test fun snapshotRemainsReadableDuringRefresh() {
        val store = SessionStore(MemoryRecords())
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val refreshing = pool.submit<String> {
                store.access(1000) {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    record(access = "new", expires = 9000)
                }
            }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            val snapshot = pool.submit<CredentialRecord> { store.snapshot() }
            try {
                assertEquals("old", snapshot.get(500, TimeUnit.MILLISECONDS).accessToken)
            } finally {
                release.countDown()
                refreshing.get(2, TimeUnit.SECONDS)
            }
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }
    @Test fun rotatingRefreshReplacesWholeRecord() { val m=MemoryRecords(); val s=SessionStore(m); s.access(1000) { record(access="new",expires=9000) }; assertEquals("refresh-new",m.value.refreshToken); assertEquals("id-new",m.value.idToken) }
    @Test fun concurrentRefreshRunsOnce() { val m=MemoryRecords(); val s=SessionStore(m); val n=AtomicInteger(); val pool=Executors.newFixedThreadPool(2); try { val jobs=(1..2).map { pool.submit<String> { s.access(1000) { n.incrementAndGet(); Thread.sleep(25); record(access="new",expires=9000) } } }; assertEquals(listOf("new","new"),jobs.map { it.get() }); assertEquals(1,n.get()) } finally { pool.shutdownNow() } }
    @Test fun atomicWriteFailurePreservesOldRecord() { val m=MemoryRecords(); val s=SessionStore(m); m.fail=true; assertThrows(java.io.IOException::class.java) { s.install(record(access="new")) }; assertEquals("old",s.snapshot().accessToken) }
    @Test fun invalidGrantClearsTokensRetainsBinding() { val s=SessionStore(MemoryRecords()); assertThrows(AuthFailure::class.java) { s.access(1000) { throw InvalidGrant() } }; assertNull(s.snapshot().refreshToken); assertEquals("issued",s.snapshot().clientId) }
    @Test fun scopeDowngradeDisablesInference() { val s=SessionStore(MemoryRecords()); assertThrows(AuthFailure::class.java) { s.access(1000) { record(access="new",scopes=setOf("openid")) } }; assertFalse(s.snapshot().canInfer) }
    @Test fun signoutClearsHintsWhenRevocationUnavailable() { val s=SessionStore(MemoryRecords()); assertFalse(s.signOut { throw java.io.IOException() }); assertNull(s.snapshot().accessToken); assertNull(s.snapshot().idToken); assertNull(s.snapshot().email); assertEquals("subject",s.snapshot().subject) }
    @Test fun confirmedRevocationReported() { val s=SessionStore(MemoryRecords()); assertTrue(s.signOut { true }); assertNull(s.snapshot().refreshToken) }
}
