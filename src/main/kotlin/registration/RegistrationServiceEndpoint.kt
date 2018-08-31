package registration

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogging
import notary.endpoint.RefundServerEndpoint

data class Response(val code: HttpStatusCode, val message: String)

/**
 * Registration HTTP service
 */
class RegistrationServiceEndpoint(
    port: Int,
    private val registrationStrategy: RegistrationStrategy
) {

    init {
        RefundServerEndpoint.logger.info { "start registration server on port $port" }

        val server = embeddedServer(Netty, port = port) {
            install(CORS)
            {
                anyHost()
                allowCredentials = true
            }
            routing {
                post("/users") {
                    val parameters = call.receiveParameters()
                    val name = parameters["name"]
                    val pubkey = parameters["pubkey"]

                    logger.info { "Registration invoked with parameters (name = \"$name\", pubkey = \"$pubkey\"" }

                    val response = onPostRegistration(name, pubkey)
                    call.respondText(response.message, status = response.code)
                }
            }
        }
        server.start(wait = false)
    }

    private fun responseError(code: HttpStatusCode, reason: String): registration.Response {
        logger.warn { "Response has been failed. $reason" }
        return registration.Response(code, "Response has been failed. $reason")
    }

    private fun onPostRegistration(name: String?, pubkey: String?): registration.Response {
        if (name == null)
            return responseError(HttpStatusCode.BadRequest, "Parameter \"name\" is not specified.")
        if (pubkey == null)
            return responseError(HttpStatusCode.BadRequest, "Parameter \"pubkey\" is not specified.")

        registrationStrategy.register(name, pubkey).fold(
            { ethWallet ->
                return registration.Response(HttpStatusCode.OK, ethWallet)
            },
            {
                // TODO - D3-121 - a.chernyshov - distinguish correct status code response (500 - server internal error)
                return responseError(HttpStatusCode.BadRequest, it.toString())
            })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
