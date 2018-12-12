@file:JvmName("BtcAddressGenerationMain")

package generation.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.getProfile
import generation.btc.config.btcAddressGenerationConfig
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["generation", "healthcheck", "provider.btc.generation", "provider.btc.network"])
class BtcAddressGenerationApplication

private val logger = KLogging().logger

fun main(args: Array<String>) {
    Result.of {
        val app = SpringApplication(BtcAddressGenerationApplication::class.java)
        app.setAdditionalProfiles(getProfile())
        app.setDefaultProperties(webPortProperties())
        app.run(*args)
    }.flatMap { context ->
        context.getBean(BtcAddressGenerationInitialization::class.java).init()
    }.failure { ex ->
        logger.error("cannot run btc address generation", ex)
        System.exit(1)
    }
}

private fun webPortProperties(): Map<String, String> {
    val properties = HashMap<String, String>()
    properties["server.port"] = btcAddressGenerationConfig.healthCheckPort.toString()
    return properties
}
