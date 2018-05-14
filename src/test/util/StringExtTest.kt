package util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import util.hexToAscii

class StringExtTest {

    @Test
    fun hexToAsciiTest() {
        assertEquals("hello hex world", "68656c6c6f2068657820776f726c64".hexToAscii())
    }

}
