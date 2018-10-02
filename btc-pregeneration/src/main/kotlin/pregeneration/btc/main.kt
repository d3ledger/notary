@file:JvmName("BtcPreGenerationMain")

package pregeneration.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import pregeneration.btc.config.btcPreGenConfig
import sidechain.iroha.IrohaInitialization

@SpringBootApplication
@ComponentScan(basePackages = ["pregeneration", "healthcheck"])
class BtcPreGenerationApplication

private val logger = KLogging().logger
fun main(args: Array<String>) {
    IrohaInitialization.loadIrohaLibrary().map {
        val app = SpringApplication(BtcPreGenerationApplication::class.java)
        app.setDefaultProperties(webPortProperties())
        app.run(*args)
    }.flatMap { context ->
        context.getBean(BtcPreGenInitialization::class.java).init()
    }.failure { ex ->
        logger.error("cannot run btc address pregeneration", ex)
        System.exit(1)
    }
}

private fun webPortProperties(): Map<String, String> {
    val properties = HashMap<String, String>()
    properties["server.port"] = btcPreGenConfig.healthCheckPort.toString()
    return properties
}
