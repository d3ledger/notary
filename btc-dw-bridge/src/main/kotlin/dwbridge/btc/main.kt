@file:JvmName("BtcDepositWithdrawalMain")

package dwbridge.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import config.getProfile
import dwbridge.btc.config.dwBridgeConfig
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KLogging
import notary.btc.init.BtcNotaryInitialization
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableMBeanExport
import sidechain.iroha.IrohaInitialization
import withdrawal.btc.init.BtcWithdrawalInitialization
import java.util.*

@EnableMBeanExport
@SpringBootApplication
@ComponentScan(
    basePackages = [
        "provider.btc.address", "provider.btc.network", "provider.btc.wallet",
        "withdrawal.btc.handler", "withdrawal.btc.init", "withdrawal.btc.provider", "withdrawal.btc.transaction",
        "listener.btc", "handler.btc", "notary.btc.init", "peer", "dwbridge", "healthcheck"]
)
class BtcDWBridgeApplication

private val logger = KLogging().logger

/**
 * Function that starts deposit and withdrawal services concurrently
 */
fun main(args: Array<String>) {
    IrohaInitialization.loadIrohaLibrary()
        .map {
            val app = SpringApplication(BtcDWBridgeApplication::class.java)
            app.setAdditionalProfiles(getProfile())
            app.setDefaultProperties(webPortProperties())
            app.run(*args)
        }.map { context ->
            // Run withdrawal service
            GlobalScope.launch {
                context.getBean(BtcWithdrawalInitialization::class.java).init()
                    .failure { ex ->
                        logger.error("Error in withdrawal service", ex)
                        System.exit(1)
                    }
            }

            // Run deposit service
            GlobalScope.launch {
                context.getBean(BtcNotaryInitialization::class.java).init().failure { ex ->
                    logger.error("Error in deposit service", ex)
                    System.exit(1)
                }
            }
        }
        .failure { ex ->
            logger.error("Cannot run btc deposit/withdrawal bridge", ex)
            System.exit(1)
        }
}

private fun webPortProperties(): Map<String, String> {
    val properties = HashMap<String, String>()
    properties["server.port"] = dwBridgeConfig.healthCheckPort.toString()
    return properties
}
