package functional

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import util.functional.end
import util.functional.endValue
import util.functional.map

class BindTest {

    /**
     * Input nullable with value
     */
    private val value: Int? = 1

    /**
     * Input nullable with null
     */
    private val nullValue: Int? = null


    /**
     * @given value as input
     * @when  map on input
     * @then  mapped value is not a null
     */
    @Test
    fun mapUsage() {
        val bound = value.map { "1" }
            .map { it + it }

        assertEquals("11", bound)
    }

    /**
     * @given null as input
     * @when  map on input
     * @then  mapped value is a null
     */
    @Test
    fun mapNullUsage() {
        val bound = nullValue.map { 1 }

        assertEquals(null, bound)
    }

    /**
     * @given value as input
     * @when  ::end operator on input
     * @then  first lambda is on call
     */
    @Test
    fun endUsage() {
        value.end({
            assert(true)
        }, {
            assert(false)
        })
    }

    /**
     * @given null as input
     * @when  ::end operator on input
     * @then  second lambda is on call
     */
    @Test
    fun endNullUsage() {
        nullValue.end({
            assert(false)
        }, {
            assert(true)
        })
    }

    /**
     * @given value as input
     * @when  ::endValue operator on input
     * @then  first lambda is on call
     */
    @Test
    fun endValueUsage() {
        val usage = value.endValue({
            assert(true)
            1
        }, {
            assert(false)
            2
        })

        assertEquals(1, usage)
    }

    /**
     * @given null as input
     * @when  ::endValue operator on input
     * @then  second lambda is on call
     */
    @Test
    fun endValueNullUsage() {
        val usage = nullValue.endValue({
            assert(false)
            1
        }, {
            assert(true)
            2
        })
        assertEquals(2, usage)
    }

}
