/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("BootstrapMain")

package jp.co.soramitsu.bootstrap

import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["jp.co.soramitsu.bootstrap"])
class Application

private val logger = KLogging().logger

fun main(args: Array<String>) {
    val app = SpringApplication(Application::class.java)
    app.run(*args)
}
