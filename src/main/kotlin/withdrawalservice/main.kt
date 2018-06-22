package withdrawalservice

import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import io.grpc.ServerBuilder
import mu.KLogging
import sideChain.iroha.IrohaBlockEmitter
import sideChain.iroha.IrohaInitializtion
import java.util.concurrent.TimeUnit


/**
 * Main entry point of Withdrawal Service app
 */
fun main(args: Array<String>) {
    val logger = KLogging()

    // TODO remove as soon as Iroha has block streamer
    // Run block emitter
    val server = ServerBuilder.forPort(8081).addService(IrohaBlockEmitter(2, TimeUnit.SECONDS)).build()
    server.start()

    IrohaInitializtion.loadIrohaLibrary()
        .flatMap { WithdrawalServiceInitialization().init() }
        .failure {
            logger.logger.error { it }
            System.exit(1)
        }

}
