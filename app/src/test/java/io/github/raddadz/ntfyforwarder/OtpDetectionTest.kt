// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.raddadz.ntfyforwarder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for OTP keyword + code extraction logic mirrored from
 * [NotificationForwarderService]. The regex constants are tested here
 * as pure logic without needing an Android context.
 */
class OtpDetectionTest {

    private val otpKeywords = Regex(
        "\\b(code|otp|verification|verify|passcode|pin|password|authenticate|token|2fa)\\b",
        RegexOption.IGNORE_CASE
    )
    private val otpCode = Regex("\\b(\\d{4,8})\\b")

    private fun extractOtpCode(text: String): String? {
        if (!otpKeywords.containsMatchIn(text)) return null
        return otpCode.find(text)?.groupValues?.get(1)
    }

    @Test fun `extracts code from OTP keyword message`() {
        assertEquals("123456", extractOtpCode("Your OTP code is 123456"))
    }

    @Test fun `extracts code with verify keyword`() {
        assertEquals("8821", extractOtpCode("Please verify with 8821"))
    }

    @Test fun `extracts code with 2fa keyword`() {
        assertEquals("441290", extractOtpCode("2FA token: 441290"))
    }

    @Test fun `returns null when no keyword present`() {
        assertNull(extractOtpCode("Meeting at 1234 Main Street"))
    }

    @Test fun `returns null when keyword present but no digit sequence`() {
        assertNull(extractOtpCode("Enter your password to continue"))
    }

    @Test fun `does not match 3-digit number as OTP code`() {
        assertNull(extractOtpCode("PIN has 3 digits: 123"))
    }

    @Test fun `does not match 9-digit number as OTP code`() {
        assertNull(extractOtpCode("Your code 123456789 is too long"))
    }

    @Test fun `extracts first code when multiple digit groups present`() {
        assertEquals("1234", extractOtpCode("code 1234 or backup 5678"))
    }

    @Test fun `keyword matching is case-insensitive`() {
        assertEquals("9922", extractOtpCode("Your CODE is 9922"))
        assertEquals("7741", extractOtpCode("OTP: 7741"))
        assertEquals("3310", extractOtpCode("Verification 3310"))
    }
}
