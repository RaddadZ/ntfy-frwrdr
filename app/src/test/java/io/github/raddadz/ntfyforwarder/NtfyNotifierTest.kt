// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.raddadz.ntfyforwarder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NtfyNotifierTest {

    // ── redactOtp ────────────────────────────────────────────────────────────

    @Test fun `redactOtp replaces 6-digit code`() {
        val result = NtfyNotifier.redactOtp("Your code is 123456")
        assertEquals("Your code is ******", result)
    }

    @Test fun `redactOtp replaces 4-digit code`() {
        val result = NtfyNotifier.redactOtp("PIN: 4821")
        assertEquals("PIN: ****", result)
    }

    @Test fun `redactOtp replaces 8-digit code`() {
        val result = NtfyNotifier.redactOtp("Token: 87654321")
        assertEquals("Token: ********", result)
    }

    @Test fun `redactOtp leaves text without digit sequences unchanged`() {
        val result = NtfyNotifier.redactOtp("No codes here")
        assertEquals("No codes here", result)
    }

    @Test fun `redactOtp does not redact 3-digit numbers`() {
        val result = NtfyNotifier.redactOtp("Price: 123")
        assertEquals("Price: 123", result)
    }

    @Test fun `redactOtp does not redact 9-digit numbers`() {
        val result = NtfyNotifier.redactOtp("Reference: 123456789")
        assertEquals("Reference: 123456789", result)
    }

    @Test fun `redactOtp replaces multiple codes in same message`() {
        val result = NtfyNotifier.redactOtp("Code 1234 and backup 5678")
        assertEquals("Code **** and backup ****", result)
    }

    @Test fun `redactOtp preserves surrounding text`() {
        val result = NtfyNotifier.redactOtp("Use code 123456 to login. Expires in 5 minutes.")
        assertEquals("Use code ****** to login. Expires in 5 minutes.", result)
    }
}
