package util

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Divide this by precision and return string representation
 */
fun BigInteger.toStringWithPrecision(precision: Short): String {
    val dividend = BigDecimal(this)
    val divisor = BigDecimal.TEN.pow(precision.toInt())

    return dividend.divide(divisor).toString()
}
