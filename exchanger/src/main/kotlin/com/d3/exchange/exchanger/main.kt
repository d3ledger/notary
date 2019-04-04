@file:JvmName("ExchangerMain")

package com.d3.exchange.exchanger

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan

private val logger = KLogging().logger

@ComponentScan(basePackages = ["com.d3.commons.exchanger"])
class ExchangerApplication

/**
 * Entry point for notary exchanger service. Performs asset conversions.
 */
fun main(args: Array<String>) {
    Result.of {
        val context = AnnotationConfigApplicationContext()
        context.register(ExchangerApplication::class.java)
        context.refresh()
        context
    }.flatMap { context ->
        context.getBean(ExchangerService::class.java).start()
    }.failure { ex ->
        logger.error("Exchanger exited with exception", ex)
        System.exit(1)
    }
}
