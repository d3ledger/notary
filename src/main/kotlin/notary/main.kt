package notary

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.natpryce.konfig.ConfigurationProperties
import io.grpc.ServerBuilder
import mu.KLogging
import sidechain.iroha.IrohaBlockEmitter
import sidechain.iroha.IrohaInitializtion
import java.util.concurrent.TimeUnit

/** Configuration parameters for notary instance */
val CONFIG = ConfigurationProperties.fromResource("defaults.properties")

/**
 * Application entry point
 */
fun main(args: Array<String>) {
    val logger = KLogging()

    //TODO remove as soon as Iroha has block streamer
    // Run block emitter
    val server = ServerBuilder.forPort(8081).addService(IrohaBlockEmitter(2, TimeUnit.SECONDS)).build()
    server.start()

    IrohaInitializtion.loadIrohaLibrary()
        .flatMap { NotaryInitialization().init() }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }
}
