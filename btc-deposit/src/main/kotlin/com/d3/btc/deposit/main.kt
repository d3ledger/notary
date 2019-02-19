@file:JvmName("BtcDepositMain")

package com.d3.btc.deposit

import com.d3.btc.deposit.config.depositConfig
import com.d3.btc.deposit.init.BtcNotaryInitialization
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.getProfile
import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import java.util.*

@SpringBootApplication
@ComponentScan(
    basePackages = [
        "com.d3.btc.deposit",
        "com.d3.btc.healthcheck",
        "com.d3.btc.provider.network",
        "com.d3.btc.provider.wallet",
        "com.d3.btc.listener",
        "com.d3.btc.handler",
        "com.d3.btc.peer"]
)
class BtcDepositApplication

private val logger = KLogging().logger

fun main(args: Array<String>) {
    Result.of {
        val app = SpringApplication(BtcDepositApplication::class.java)
        app.setAdditionalProfiles(getProfile())
        app.setDefaultProperties(webPortProperties())
        app.run(*args)
    }.flatMap { context ->
        context.getBean(BtcNotaryInitialization::class.java).init()
    }.failure { ex ->
        logger.error("Cannot run btc deposit", ex)
        System.exit(1)
    }
}

private fun webPortProperties(): Map<String, String> {
    val properties = HashMap<String, String>()
    properties["server.port"] = depositConfig.healthCheckPort.toString()
    return properties
}
