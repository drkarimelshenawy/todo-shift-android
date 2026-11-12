package com.todoshift.app.v11

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SecurityAttemptLimitTest {
    private val prefs get() = RuntimeEnvironment.getApplication().getSharedPreferences("todo_shift_security_v11", Context.MODE_PRIVATE)
    private var time = 1_000_000L
    private fun store() = V11SecurityStore(prefs) { time }
    private fun wrongFive(s: V11SecurityStore) { repeat(5) { assertFalse(s.verifyPin("0000")) } }

    @Test fun correctPinIsBlockedAfterFiveFailuresAndWorksAfterDelay() {
        val s = store(); assertTrue(s.setPin("4826")); wrongFive(s)
        assertEquals(30_000L, s.verificationDelayMillis())
        assertFalse(s.verifyPin("4826"))
        time += 30_000L
        assertTrue(s.verifyPin("4826"))
        assertEquals(0L, s.verificationDelayMillis())
    }

    @Test fun recreatedStoreKeepsTheSameDelay() {
        val s = store(); s.setPin("4826"); wrongFive(s)
        assertFalse(store().verifyPin("4826"))
        assertEquals(30_000L, store().verificationDelayMillis())
    }

    @Test fun pinAndPatternShareOneAttemptBudget() {
        val s = store(); s.setPin("4826"); s.setPattern(listOf(0, 1, 2, 3))
        wrongFive(s)
        assertFalse(store().verifyPattern(listOf(0, 1, 2, 3)))
        time += 30_000L
        assertTrue(store().verifyPattern(listOf(0, 1, 2, 3)))
    }

    @Test fun repeatedFailuresIncreaseDelayButNeverPermanentlyLockTheOwner() {
        val s = store(); s.setPin("4826"); wrongFive(s)
        for (expected in listOf(60_000L, 120_000L, 240_000L, 300_000L, 300_000L)) {
            time += s.verificationDelayMillis()
            assertFalse(s.verifyPin("0000"))
            assertEquals(expected, s.verificationDelayMillis())
        }
        time += s.verificationDelayMillis()
        assertTrue(s.verifyPin("4826"))
    }

    @Test fun blockedTapsDoNotExtendTheDelay() {
        val s = store(); s.setPin("4826"); wrongFive(s)
        time += 5_000L
        repeat(10) { assertFalse(store().verifyPin("0000")) }
        assertEquals(25_000L, s.verificationDelayMillis())
    }

    @Test fun successfulVerificationResetsFailureBudget() {
        val s = store(); s.setPin("4826")
        repeat(4) { assertFalse(s.verifyPin("0000")) }
        assertTrue(s.verifyPin("4826"))
        repeat(4) { assertFalse(s.verifyPin("0000")) }
        assertEquals(0L, s.verificationDelayMillis())
        assertTrue(s.verifyPin("4826"))
    }

    @Test fun backwardsClockChangeHasBoundedRecovery() {
        val s = store(); s.setPin("4826"); wrongFive(s)
        time -= 86_400_000L
        assertEquals(300_000L, s.verificationDelayMillis())
        time += 300_000L
        assertTrue(s.verifyPin("4826"))
    }

    @Test fun failedDurableAcknowledgementCannotUnlock() {
        val s = store(); s.setPin("4826")
        val failing = object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val editor = prefs.edit()
                return object : SharedPreferences.Editor by editor {
                    override fun commit() = false
                    override fun remove(key: String?): SharedPreferences.Editor { editor.remove(key); return this }
                }
            }
        }
        assertFalse(V11SecurityStore(failing) { time }.verifyPin("4826"))
    }
}
