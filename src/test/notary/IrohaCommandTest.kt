package notary

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert
import org.junit.Test

/**
 * Test accessor of a data class
 */
class IrohaCommandTest {

    /** Test an ability to mock a final class */
    @Test
    fun testCommandAddAssetQuantity() {
        val expected = "I can mock final classes"
        val m = mock<IrohaCommand.CommandAddAssetQuantity>() {
            on { accountId } doReturn expected
        }

        Assert.assertEquals(expected, m.accountId)
    }
}
