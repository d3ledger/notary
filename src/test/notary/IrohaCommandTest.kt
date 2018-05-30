package notary

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

        assertEquals(expected, m.accountId)
    }

    @Test
    fun test() {
        val obs = Observable.just(1, 2, 3, 4, 5, 6)

        obs
            .groupBy { it % 2 == 0 }
            .map {
                //it.key
                if (it.key!!)
                    it.map { it * 10 }
                else
                    it.map { it * 100 }
//                it.ignoreElements()
            }
            .flatMap { it }
            .subscribe { println(it) }
    }
}
