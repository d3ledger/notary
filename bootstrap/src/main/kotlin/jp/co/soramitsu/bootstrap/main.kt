
@file:JvmName("BootstrapMain")

package jp.co.soramitsu.bootstrap

import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["jp.co.soramitsu.bootstrap"])

class main

    private val logger = KLogging().logger

    fun main(args: Array<String>) {
        val app = SpringApplication(main::class.java)
        app.run(*args)
    }


