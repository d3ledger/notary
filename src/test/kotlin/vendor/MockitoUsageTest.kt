package vendor

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

interface Dependency {
    fun foo(a: Int): String
}

class MockitoUsage {
    @Test
    fun mockitoUsage() {
        val mock = mock<Dependency> {
            on { foo(any()) } doReturn "2"
            on { foo(1) } doReturn "1"
        }

        assertEquals("1", mock.foo(1))
        assertEquals("2", mock.foo(555))
    }
}
