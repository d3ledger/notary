@file:JvmName("BtcFreeAddressGeneration")

package generation.btc.trigger

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import provider.btc.address.BtcAddressType

@ComponentScan(basePackages = ["generation.btc.trigger"])
class BtcFreeAddressGenerationApplication

private val logger = KLogging().logger
/*
   Free address generation entry point
 */
fun main(args: Array<String>) {
    val addressType = BtcAddressType.FREE
    Result.of {
        AnnotationConfigApplicationContext(BtcFreeAddressGenerationApplication::class.java)
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
