package com.d3.btc.dwbridge.config

import com.d3.commons.config.BitcoinConfig
import com.d3.commons.config.IrohaConfig

interface BtcDWBridgeConfig {

    val bitcoin: BitcoinConfig

    val iroha: IrohaConfig

    val healthCheckPort: Int
}
