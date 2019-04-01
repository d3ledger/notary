@file:JvmName("BtcAddressGenerationMain")

package com.d3.btc.generation

import com.d3.btc.generation.init.BtcAddressGenerationInitialization
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.d3.commons.config.getProfile
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan

const val BTC_ADDRESS_GENERATION_SERVICE_NAME = "btc-add-gen"

@ComponentScan(
    basePackages = [
        "com.d3.btc.generation",
        "com.d3.btc.healthcheck",
        "com.d3.btc.provider.generation",
        "com.d3.btc.provider.network",
        "com.d3.btc.generation.trigger"]
)
class BtcAddressGenerationApplication

private val logger = KLogging().logger

fun main(args: Array<String>) {
    Result.of {
        val context = AnnotationConfigApplicationContext()
        context.environment.setActiveProfiles(getProfile())
        context.register(BtcAddressGenerationApplication::class.java)
        context.refresh()
        context
    }.flatMap { context ->
        context.getBean(BtcAddressGenerationInitialization::class.java).init()
    }.failure { ex ->
        logger.error("cannot run btc address generation", ex)
        System.exit(1)
    }
}
