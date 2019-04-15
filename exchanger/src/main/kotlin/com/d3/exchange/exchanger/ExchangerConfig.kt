package com.d3.exchange.exchanger

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.IrohaCredentialConfig

/**
 * Interface represents configs for exchanger service
 */
interface ExchangerConfig {

    /** Iroha configs */
    val iroha: IrohaConfig

    /** Iroha registration service credential */
    val irohaCredential: IrohaCredentialConfig

    /** RMQ queue name */
    val irohaBlockQueue: String

    /** Liquidity providers account names */
    val liquidityProviders: String
}
