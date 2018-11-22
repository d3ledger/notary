@file:JvmName("BtcFreeAddressGeneration")

package generation.btc.trigger

import generation.btc.config.btcAddressGenerationConfig
import provider.btc.address.BtcAddressType

// Generates Bitcoin free address that can be registered later
fun main(args: Array<String>) {
    startAddressGeneration(btcAddressGenerationConfig, BtcAddressType.FREE)
}
