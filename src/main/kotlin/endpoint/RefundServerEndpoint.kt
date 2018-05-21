package endpoint

import com.squareup.moshi.Moshi
import endpoint.eth.EthNotaryResponse
import endpoint.eth.EthNotaryResponseMoshiAdapter
import endpoint.eth.EthRefundContract
import endpoint.eth.EthRefundStrategy
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogging
import util.functional.endValue
import util.functional.map
import util.functional.truePredicate

/**
 * Class is waiting for custodian's intention for rollback
 */
class RefundServerEndpoint(
    private val serverBundle: ServerInitializationBundle,
    private val ethStrategy: EthRefundStrategy
) {

    private val moshi = Moshi.Builder().add(EthNotaryResponseMoshiAdapter()).build()!!
    private val ethRefundAdapter = moshi.adapter(EthRefundContract::class.java)!!
    private val ethNotaryAdapter = moshi.adapter(EthNotaryResponse::class.java)!!

    init {
        val server = embeddedServer(Netty, port = serverBundle.port) {
            routing {
                get(serverBundle.ethRefund) {
                    logger.info { "EthRefund invoked" }
                    logger.info { "parameters:${call.parameters}" }
                    call.respondText { onCallEthRefund(call.parameters["contract"]) }
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
        return rawRequest
            .map(ethRefundAdapter::fromJson)
            .truePredicate(ethStrategy::validate)
            .map(ethStrategy::performRefund)
            .map(ethNotaryAdapter::toJson)
            .endValue({
                it
            }, {
                onErrorPipelineCall()
            })
    }

    /**
     * Method return response on stateless invalid request
     */
    fun onErrorPipelineCall(): String {
        logger.error { "Request has been failed" }
        return ""
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
