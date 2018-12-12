@file:JvmName("BtcNotaryMain")

package notary.btc

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.getProfile
import mu.KLogging
import notary.btc.config.notaryConfig
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import java.util.*

@SpringBootApplication
@ComponentScan(basePackages = ["notary", "healthcheck", "provider.btc.network", "provider.btc.wallet", "listener.btc", "handler.btc"])
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
    }
        .failure { ex ->
            logger.error("Cannot run btc notary", ex)
            System.exit(1)
        }
}

private fun webPortProperties(): Map<String, String> {
    val properties = HashMap<String, String>()
    properties["server.port"] = notaryConfig.healthCheckPort.toString()
    return properties
}
