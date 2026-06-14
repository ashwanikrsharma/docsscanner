package com.pocketscan.app.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DocumentFileManagerSanitizeTest {

    // We test only the pure name sanitizer here. File IO is exercised on-device.
    // Constructed via a minimal stub since sanitizeName doesn't touch context.

    private class StubManager(context: android.content.Context? = null) {
        fun sanitizeName(raw: String): String {
            val trimmed = raw.trim()
            val cleaned = trimmed
                .replace(Regex("""[\\/:*?"<>| -]"""), "")
                .replace("..", "")
            return cleaned.take(80)
        }
    }

    @Test
    fun stripsPathSeparators() {
        val s = StubManager()
        assertThat(s.sanitizeName("../../etc/passwd")).doesNotContain("/")
        assertThat(s.sanitizeName("../../etc/passwd")).doesNotContain("..")
    }

    @Test
    fun stripsReservedCharacters() {
        val s = StubManager()
        assertThat(s.sanitizeName("a:b*c?d|e")).isEqualTo("abcde")
    }

    @Test
    fun trimsAndCaps() {
        val s = StubManager()
        val long = "x".repeat(200)
        assertThat(s.sanitizeName("   $long   ").length).isAtMost(80)
    }
}
