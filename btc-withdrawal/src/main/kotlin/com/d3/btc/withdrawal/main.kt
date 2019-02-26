@file:JvmName("BtcWithdrawalMain")

package com.d3.btc.withdrawal

import com.d3.btc.withdrawal.init.BtcWithdrawalInitialization
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import config.getProfile
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableMBeanExport

@EnableMBeanExport
@ComponentScan(
    basePackages = [
        "com.d3.btc.wallet",
        "com.d3.btc.withdrawal",
        "com.d3.btc.healthcheck",
        "com.d3.btc.provider.network",
        "com.d3.btc.handler",
        "com.d3.btc.provider.address",
        "com.d3.btc.peer"]
)
class BtcWithdrawalApplication

private val logger = KLogging().logger

fun main(args: Array<String>) {
    Result.of {
        val context = AnnotationConfigApplicationContext()
        context.environment.setActiveProfiles(getProfile())
        context.register(BtcWithdrawalApplication::class.java)
        context.refresh()
        context
    }.flatMap { context ->
        context.getBean(BtcWithdrawalInitialization::class.java).init()
    }.failure { ex ->
        logger.error("Cannot run btc withdrawal", ex)
        System.exit(1)
    }
}
