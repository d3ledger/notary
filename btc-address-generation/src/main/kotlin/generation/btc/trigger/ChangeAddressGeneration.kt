@file:JvmName("BtcChangeAddressGeneration")

package generation.btc.trigger

import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import provider.btc.address.BtcAddressType
import sidechain.iroha.IrohaInitialization

@ComponentScan(basePackages = ["generation.btc.trigger"])
class BtcChangeAddressGenerationApplication

private val logger = KLogging().logger
/*
   Change address generation entry point
 */
fun main(args: Array<String>) {
    val addressType = BtcAddressType.CHANGE
    IrohaInitialization.loadIrohaLibrary().map {
        AnnotationConfigApplicationContext(BtcChangeAddressGenerationApplication::class.java)
    }.flatMap { context ->
        context.getBean(AddressGenerationTrigger::class.java).startAddressGeneration(addressType)
    }.fold(
        {
            logger.info { "${addressType.title} address generation process was started" }
        }, { ex ->
            logger.error("Cannot start ${addressType.title} address generation process", ex)
            System.exit(1)
        })
}
