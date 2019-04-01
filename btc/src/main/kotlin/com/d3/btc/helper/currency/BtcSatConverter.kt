package com.d3.btc.helper.currency

import java.math.BigDecimal

// 1 BTC is 100_000_000 SAT
private val SAT_IN_BTC = BigDecimal.valueOf(100_000_000)

/**
 * Converts Bitcoins into Satoshis
 * @param btc - amount of Bitcoins
 */
fun btcToSat(btc: BigDecimal) = btc.multiply(SAT_IN_BTC).toLong()


/**
 * Converts Satoshis into Bitcoins
 * @param sat - amount of Satoshis
 */
fun satToBtc(sat: Long) = BigDecimal.valueOf(sat).divide(SAT_IN_BTC)
