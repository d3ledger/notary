package com.d3.btc.dwbridge.config

import config.BitcoinConfig
import config.IrohaConfig

interface BtcDWBridgeConfig {

    val bitcoin: BitcoinConfig

    val iroha: IrohaConfig

    val healthCheckPort: Int
}
