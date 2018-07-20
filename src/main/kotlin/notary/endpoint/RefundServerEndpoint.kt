package notary.endpoint

import com.squareup.moshi.Moshi
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogging
import notary.endpoint.eth.*
import util.functional.bind
import util.functional.endValue
import java.math.BigInteger

/**
 * Class is waiting for custodian's intention for rollback
 */
class RefundServerEndpoint(
    private val serverBundle: ServerInitializationBundle,
    private val ethStrategy: EthRefundStrategy
) {

    private val moshi = Moshi
        .Builder()
        .add(EthNotaryResponseMoshiAdapter())
        .add(BigInteger::class.java, BigIntegerMoshiAdapter())
        .build()!!
    private val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!

    init {
        logger.info { "start refund server on port ${serverBundle.port}" }

        val server = embeddedServer(Netty, port = serverBundle.port) {
            install(CORS)
            {
                anyHost()
                allowCredentials = true
            }
            routing {
                get(serverBundle.ethRefund + "/{tx_hash}") {
                    logger.info { "EthRefund invoked with parameters:${call.parameters}" }
                    call.respondText { onCallEthRefund(call.parameters["tx_hash"]) }
                }
            }
        }
        server.start(wait = false)
    }

    /**
     * Method that call of raw ETH refund request
     * @param rawRequest - raw string of request
     */
    fun onCallEthRefund(rawRequest: String?): String {
        return createRequest(rawRequest)
            .bind(ethStrategy::performRefund)
            .bind(ethNotaryAdapter::toJson)
            .endValue({
                it
            }, {
                onErrorPipelineCall()
            })
    }

    /**
     * Creates a [EthRefundRequest] object from request string
     */
    private fun createRequest(txHash: String?): EthRefundRequest? {
        return txHash?.let { EthRefundRequest(it) }
    }

    /**
     * Method return response on stateless invalid request
     */
    private fun onErrorPipelineCall(): String {
        logger.error { "Request has been failed" }
        return "Request has been failed. Error in URL"
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
