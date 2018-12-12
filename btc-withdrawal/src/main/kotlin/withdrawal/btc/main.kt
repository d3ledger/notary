@file:JvmName("BtcWithdrawalMain")

package withdrawal.btc

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import config.getProfile
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableMBeanExport
import sidechain.iroha.IrohaInitialization
import util.createFolderIfDoesntExist
import withdrawal.btc.config.withdrawalConfig
import withdrawal.btc.init.BtcWithdrawalInitialization
import java.util.*

@EnableMBeanExport
@SpringBootApplication
@ComponentScan(basePackages = ["withdrawal", "healthcheck", "provider.btc.network", "handler.btc", "provider.btc.address", "provider.btc.wallet", "peer"])
class BtcWithdrawalApplication

private val logger = KLogging().logger

fun main(args: Array<String>) {
    IrohaInitialization.loadIrohaLibrary()
        .map {
            // Create block storage folder
            createFolderIfDoesntExist(withdrawalConfig.bitcoin.blockStoragePath)
        }
        .map {
            val app = SpringApplication(BtcWithdrawalApplication::class.java)
            app.setAdditionalProfiles(getProfile())
            app.setDefaultProperties(webPortProperties())
            app.run(*args)
        }.flatMap { context ->
            context.getBean(BtcWithdrawalInitialization::class.java).init()
        }
        .failure { ex ->
            logger.error("Cannot run btc withdrawal", ex)
            System.exit(1)
        }
}

private fun webPortProperties(): Map<String, String> {
    val properties = HashMap<String, String>()
    properties["server.port"] = withdrawalConfig.healthCheckPort.toString()
    return properties
}
