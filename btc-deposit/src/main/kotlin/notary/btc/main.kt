@file:JvmName("BtcNotaryMain")

package notary.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.getProfile
import mu.KLogging
import notary.btc.config.notaryConfig
import notary.btc.init.BtcNotaryInitialization
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import java.util.*

@SpringBootApplication
@ComponentScan(
    basePackages = [
        "notary",
        "com.d3.btc.healthcheck",
        "com.d3.btc.provider.network",
        "com.d3.btc.provider.wallet",
        "com.d3.btc.listener",
        "com.d3.btc.handler",
        "com.d3.btc.peer"]
)
class BtcNotaryApplication

private val logger = KLogging().logger

fun main(args: Array<String>) {
    Result.of {
        val app = SpringApplication(BtcNotaryApplication::class.java)
        app.setAdditionalProfiles(getProfile())
        app.setDefaultProperties(webPortProperties())
        app.run(*args)
    }.flatMap { context ->
        context.getBean(BtcNotaryInitialization::class.java).init()
    }.failure { ex ->
        logger.error("Cannot run btc notary", ex)
        System.exit(1)
    }
}

private fun webPortProperties(): Map<String, String> {
    val properties = HashMap<String, String>()
    properties["server.port"] = notaryConfig.healthCheckPort.toString()
    return properties
}
