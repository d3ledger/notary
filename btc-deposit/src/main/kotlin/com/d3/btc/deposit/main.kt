/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("BtcDepositMain")

package com.d3.btc.deposit

import com.d3.btc.deposit.init.BtcNotaryInitialization
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.d3.commons.config.getProfile
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.EnableMBeanExport

const val BTC_DEPOSIT_SERVICE_NAME="btc-deposit"

@EnableMBeanExport
@ComponentScan(
    basePackages = [
        "com.d3.btc.wallet",
        "com.d3.btc.deposit",
        "com.d3.btc.healthcheck",
        "com.d3.btc.provider.network",
        "com.d3.btc.listener",
        "com.d3.btc.handler",
        "com.d3.btc.peer"]
)
class BtcDepositApplication

private val logger = KLogging().logger

fun main(args: Array<String>) {
    Result.of {
        val context = AnnotationConfigApplicationContext()
        context.environment.setActiveProfiles(getProfile())
        context.register(BtcDepositApplication::class.java)
        context.refresh()
        context
    }.flatMap { context ->
        context.getBean(BtcNotaryInitialization::class.java).init()
    }.failure { ex ->
        logger.error("Cannot run btc deposit", ex)
        System.exit(1)
    }
}
