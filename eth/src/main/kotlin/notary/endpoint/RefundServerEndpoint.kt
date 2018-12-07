package notary.endpoint

import com.squareup.moshi.Moshi
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogging
import notary.endpoint.eth.*
import java.math.BigInteger

data class Response(val code: HttpStatusCode, val message: String)

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
        logger.info { "Start refund server on port ${serverBundle.port}" }

        val server = embeddedServer(Netty, port = serverBundle.port) {
            install(CORS)
            {
                anyHost()
                allowCredentials = true
            }
            routing {
                get(serverBundle.ethRefund + "/{tx_hash}") {
                    logger.info { "Eth refund invoked with parameters:${call.parameters}" }
                    val response = onCallEthRefund(call.parameters["tx_hash"])
                    call.respondText(response.message, status = response.code)
                }
            }
        }
        server.start(wait = false)
    }

    /**
     * Method that call of raw ETH refund request
     * @param rawRequest - raw string of request
     */
    fun onCallEthRefund(rawRequest: String?): Response {
        return rawRequest?.let { request ->
            val response = ethStrategy.performRefund(EthRefundRequest(request))
            when (response) {
                is EthNotaryResponse.Successful -> Response(
                    HttpStatusCode.OK,
                    ethNotaryAdapter.toJson(response)
                )
                is EthNotaryResponse.Error -> {
                    logger.error { response.reason }
                    Response(HttpStatusCode.BadRequest, ethNotaryAdapter.toJson(response))
                }
            }
        } ?: onErrorPipelineCall()
    }

    /**
     * Method return response on stateless invalid request
     */
    private fun onErrorPipelineCall(): Response {
        logger.error { "Request has been failed" }
        return Response(HttpStatusCode.BadRequest, "Request has been failed. Error in URL")
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
