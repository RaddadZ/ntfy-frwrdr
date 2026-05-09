// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.raddadz.ntfyforwarder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDataStoreTest {

    @Test fun `https url is secure`() {
        assertTrue(SettingsDataStore.isUrlSecure("https://ntfy.example.com"))
    }

    @Test fun `https with path is secure`() {
        assertTrue(SettingsDataStore.isUrlSecure("https://ntfy.example.com/topic"))
    }

    @Test fun `plain http is insecure`() {
        assertFalse(SettingsDataStore.isUrlSecure("http://ntfy.example.com"))
    }

    @Test fun `localhost http is allowed`() {
        assertTrue(SettingsDataStore.isUrlSecure("http://localhost:8080"))
    }

    @Test fun `loopback 127 is allowed`() {
        assertTrue(SettingsDataStore.isUrlSecure("http://127.0.0.1:2586"))
    }

    @Test fun `rfc1918 class A 10-dot is allowed`() {
        assertTrue(SettingsDataStore.isUrlSecure("http://10.0.0.1:8080"))
    }

    @Test fun `rfc1918 class B 172-dot is allowed`() {
        assertTrue(SettingsDataStore.isUrlSecure("http://172.16.0.1"))
        assertTrue(SettingsDataStore.isUrlSecure("http://172.31.255.254"))
    }

    @Test fun `rfc1918 class C 192-168 is allowed`() {
        assertTrue(SettingsDataStore.isUrlSecure("http://192.168.1.100"))
    }

    @Test fun `apipa link-local is allowed`() {
        assertTrue(SettingsDataStore.isUrlSecure("http://169.254.1.1"))
    }

    @Test fun `ipv6 loopback is allowed`() {
        assertTrue(SettingsDataStore.isUrlSecure("http://[::1]:8080"))
    }

    @Test fun `empty url returns false (caller must guard with isNotBlank)`() {
        assertFalse(SettingsDataStore.isUrlSecure(""))
    }

    @Test fun `url with leading whitespace is handled`() {
        assertTrue(SettingsDataStore.isUrlSecure("  https://ntfy.example.com"))
        assertFalse(SettingsDataStore.isUrlSecure("  http://ntfy.example.com"))
    }
}
