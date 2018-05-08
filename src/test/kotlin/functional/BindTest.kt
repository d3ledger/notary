package functional

import org.junit.Assert
import org.junit.Test
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

        Assert.assertEquals("11", bound)
    }

    /**
     * @given null as input
     * @when  map on input
     * @then  mapped value is a null
     */
    @Test
    fun mapNullUsage() {
        val bound = nullValue.map { 1 }

        Assert.assertEquals(null, bound)
    }

    /**
     * @given value as input
     * @when  ::end operator on input
     * @then  first lambda is on call
     */
    @Test
    fun endUsage() {
        value.end({
            Assert.assertTrue(true)
        }, {
            Assert.assertTrue(false)
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
            Assert.assertTrue(false)
        }, {
            Assert.assertTrue(true)
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
            Assert.assertTrue(true)
            1
        }, {
            Assert.assertTrue(false)
            2
        })

        Assert.assertEquals(1, usage)
    }

    /**
     * @given null as input
     * @when  ::endValue operator on input
     * @then  second lambda is on call
     */
    @Test
    fun endValueNullUsage() {
        val usage = nullValue.endValue({
            Assert.assertTrue(false)
            1
        }, {
            Assert.assertTrue(true)
            2
        })
        Assert.assertEquals(2, usage)
    }

}
