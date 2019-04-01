@file:JvmName("BtcDepositWithdrawalMain")

package com.d3.btc.dwbridge

import com.d3.btc.deposit.init.BtcNotaryInitialization
import com.d3.btc.dwbridge.config.dwBridgeConfig
import com.d3.btc.withdrawal.init.BtcWithdrawalInitialization
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import com.d3.commons.config.getProfile
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableMBeanExport
import com.d3.commons.util.createFolderIfDoesntExist

const val BTC_DW_BRIDGE_SERVICE_NAME = "btc-dw-bridge"

@EnableMBeanExport
@ComponentScan(
    basePackages = [
        "com.d3.btc.wallet",
        "com.d3.btc.provider.address",
        "com.d3.btc.provider.network",
        "com.d3.btc.withdrawal.handler",
        "com.d3.btc.withdrawal.service",
        "com.d3.btc.withdrawal.init",
        "com.d3.btc.withdrawal.provider",
        "com.d3.btc.withdrawal.transaction",
        "com.d3.btc.listener",
        "com.d3.btc.handler",
        "com.d3.btc.deposit.init",
        "com.d3.btc.deposit.service",
        "com.d3.btc.peer",
        "com.d3.btc.dwbridge",
        "com.d3.btc.healthcheck"]
)
class BtcDWBridgeApplication

private val logger = KLogging().logger

/**
 * Function that starts deposit and withdrawal services concurrently
 */
fun main(args: Array<String>) {
    Result.of {
        // Create block storage folder
        createFolderIfDoesntExist(dwBridgeConfig.bitcoin.blockStoragePath)
    }.map {
        val context = AnnotationConfigApplicationContext()
        context.environment.setActiveProfiles(getProfile())
        context.register(BtcDWBridgeApplication::class.java)
        context.refresh()
        context
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
    }.failure { ex ->
        logger.error("Cannot run btc deposit/withdrawal bridge", ex)
        System.exit(1)
    }
}
