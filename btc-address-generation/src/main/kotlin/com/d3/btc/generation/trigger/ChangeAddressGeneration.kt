/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("BtcChangeAddressGeneration")

package com.d3.btc.generation.trigger

import com.d3.btc.model.BtcAddressType
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import mu.KLogging
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.ComponentScan

@ComponentScan(basePackages = ["com.d3.btc.generation.trigger"])
class BtcChangeAddressGenerationApplication

private val logger = KLogging().logger
/*
   Change address generation entry point
 */
fun main(args: Array<String>) {
    val addressType = BtcAddressType.CHANGE
    Result.of {
        AnnotationConfigApplicationContext(BtcChangeAddressGenerationApplication::class.java)
    }.flatMap { context ->
        context.getBean(AddressGenerationTrigger::class.java)
            .startAddressGeneration(
                addressType = addressType,
                nodeId = btcAddressGenerationTriggerConfig.nodeId
            )
    }.fold(
        {
            logger.info { "${addressType.title} address generation process was started" }
        }, { ex ->
            logger.error("Cannot start ${addressType.title} address generation process", ex)
            System.exit(1)
        })
}
