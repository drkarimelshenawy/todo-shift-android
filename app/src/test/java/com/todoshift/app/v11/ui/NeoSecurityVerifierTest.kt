package com.todoshift.app.v11.ui

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NeoSecurityVerifierTest {
    @Test fun verificationRunsOnWorkerAndResultReturnsToCallerScope() = runBlocking {
        val caller = Thread.currentThread()
        val verifier = NeoSecurityVerifier(this)
        var result: Boolean? = null
        verifier.submit({ assertNotSame(caller, Thread.currentThread()); true }) {
            assertSame(caller, Thread.currentThread())
            result = it
        }
        withTimeout(10_000L) { while (verifier.busy) yield() }
        assertEquals(true, result)
    }

    @Test fun repeatedTapWhileBusyDoesNotQueueAnotherCheck() = runBlocking {
        val release = CountDownLatch(1)
        val checks = AtomicInteger()
        val verifier = NeoSecurityVerifier(this)
        verifier.submit({ checks.incrementAndGet(); check(release.await(5, TimeUnit.SECONDS)); true }) {}
        verifier.submit({ checks.incrementAndGet(); true }) { fail("Second tap must be ignored") }
        release.countDown()
        withTimeout(10_000L) { while (verifier.busy) yield() }
        assertEquals(1, checks.get())
    }

    @Test fun disposedScopeCannotDeliverAnUnlockCallback() = runBlocking {
        val owner = CoroutineScope(coroutineContext + Job())
        val verifier = NeoSecurityVerifier(owner)
        var delivered = false
        verifier.submit({ true }) { delivered = true }
        owner.cancel()
        yield()
        assertFalse(delivered)
    }

    @Test fun failedCheckDoesNotGrantAccessAndAllowsRetry() = runBlocking {
        val verifier = NeoSecurityVerifier(this)
        var result: Boolean? = null
        verifier.submit({ throw IllegalStateException("Storage unavailable") }) { result = it }
        withTimeout(10_000L) { while (verifier.busy) yield() }
        assertEquals(false, result)
        verifier.submit({ true }) { result = it }
        withTimeout(10_000L) { while (verifier.busy) yield() }
        assertEquals(true, result)
    }
}
