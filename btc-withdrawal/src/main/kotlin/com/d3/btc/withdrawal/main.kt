@file:JvmName("BtcWithdrawalMain")

package com.d3.btc.withdrawal

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.getProfile
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableMBeanExport
import com.d3.btc.withdrawal.config.withdrawalConfig
import com.d3.btc.withdrawal.init.BtcWithdrawalInitialization

@EnableMBeanExport
@SpringBootApplication
@ComponentScan(
    basePackages = [
        "com.d3.btc.withdrawal",
        "com.d3.btc.healthcheck",
        "com.d3.btc.provider.network",
        "com.d3.btc.handler",
        "com.d3.btc.provider.address",
        "com.d3.btc.provider.wallet",
        "com.d3.btc.peer"]
)
class BtcWithdrawalApplication

private val logger = KLogging().logger

fun main(args: Array<String>) {
    Result.of {
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
