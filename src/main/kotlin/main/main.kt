package main

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.natpryce.konfig.ConfigurationProperties
import mu.KLogging
import notary.NotaryInitialization
import sideChain.iroha.IrohaInitializtion

/** Configuration parameters for notary instance */
val CONFIG = ConfigurationProperties.fromResource("defaults.properties")

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val logger = KLogging()
    val notary = NotaryInitialization()

    IrohaInitializtion.loadIrohaLibrary()
        .flatMap { notary.init() }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
