package util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BigIntegerExtTest {

    /**
     * @given number 1 and precision 4
     * @when call toStringWithPrecision()
     * @then get "0.0001" string
     */
    @Test
    fun toStringWithPrecisionTest() {
        Assertions.assertEquals("0.0001", BigInteger.ONE.toStringWithPrecision(4))
    }

    /**
     * @given number 1 and precision 0
     * @when call toStringWithPrecision()
     * @then get "0" string
     */
    @Test
    fun toStringWithPrecisionZero() {
        Assertions.assertEquals("1", BigInteger.ONE.toStringWithPrecision(0))
    }
}
