package main

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.natpryce.konfig.ConfigurationProperties
import io.grpc.ServerBuilder
import mu.KLogging
import notary.NotaryInitialization
import sideChain.iroha.IrohaBlockEmitter
import sideChain.iroha.IrohaInitializtion
import java.util.concurrent.TimeUnit

/** Configuration parameters for notary instance */
val CONFIG = ConfigurationProperties.fromResource("defaults.properties")

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val logger = KLogging()
    val notary = NotaryInitialization()

    //TODO remove as soon as Iroha has block streamer

    val server = ServerBuilder.forPort(8081).addService(IrohaBlockEmitter(2, TimeUnit.SECONDS)).build()
    server.start()

    // Run block emitter
    IrohaInitializtion.loadIrohaLibrary()
        .flatMap { notary.init() }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
