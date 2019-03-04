@file:JvmName("Bootstrap")

package jp.co.soramitsu.bootstrap

import mu.KLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["jp.co.soramitsu.bootstrap"])
class BootstrapMain

    private val logger = KLogging().logger

    fun main(args: Array<String>) {
        val app = SpringApplication(BootstrapMain::class.java)
        app.run(*args)
    }


