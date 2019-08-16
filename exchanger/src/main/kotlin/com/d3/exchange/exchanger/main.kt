/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("ExchangerMain")

package com.d3.exchange.exchanger

import com.d3.exchange.exchanger.service.ExchangerService
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import kotlin.system.exitProcess

private val logger = KLogging().logger

@ComponentScan(basePackages = ["com.d3.exchange"])
class ExchangerApplication

/**
 * Entry point for notary exchanger service. Performs asset conversions.
 */
fun main() {
    Result.of {
        val context = AnnotationConfigApplicationContext()
        context.register(ExchangerApplication::class.java)
        context.refresh()
        context
    }.flatMap { context ->
        context.getBean(ExchangerService::class.java).start()
    }.failure { ex ->
        logger.error("Exchanger exited with an exception", ex)
        exitProcess(1)
    }
}
