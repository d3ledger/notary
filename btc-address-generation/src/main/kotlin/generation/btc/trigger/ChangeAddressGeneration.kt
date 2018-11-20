@file:JvmName("BtcChangeAddressGeneration")

package generation.btc.trigger

import generation.btc.config.btcAddressGenerationConfig
import provider.btc.address.BtcAddressType

// Generate Bitcoin change address
fun main(args: Array<String>) {
    startAddressGeneration(btcAddressGenerationConfig, BtcAddressType.CHANGE)
}
