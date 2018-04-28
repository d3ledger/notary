package vendor

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert
import org.junit.Test

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

        Assert.assertEquals("1", mock.foo(1))
        Assert.assertEquals("2", mock.foo(555))
    }
}
