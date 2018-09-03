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

data class Response(val code: HttpStatusCode, val message: String)

/**
 * Registration HTTP service
 */
class RegistrationServiceEndpoint(
    port: Int,
    private val registrationStrategy: RegistrationStrategy
) {

    init {
        logger.info { "Start registration server on port $port" }

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

    private fun responseError(code: HttpStatusCode, reason: String): Response {
        logger.warn { "Response has been failed. $reason" }
        return Response(code, "Response has been failed. $reason")
    }

    private fun onPostRegistration(name: String?, pubkey: String?): Response {
        if (name == null && pubkey == null) {
            return responseError(HttpStatusCode.BadRequest, "Parameters \"name\" and \"pubkey\" are not specified.")
        } else if (name == null)
            return responseError(HttpStatusCode.BadRequest, "Parameter \"name\" is not specified.")
        else if (pubkey == null)
            return responseError(HttpStatusCode.BadRequest, "Parameter \"pubkey\" is not specified.")

        registrationStrategy.register(name, pubkey).fold(
            { ethWallet ->
                return Response(HttpStatusCode.OK, ethWallet)
            },
            { ex ->
                // TODO - D3-121 - a.chernyshov - distinguish correct status code response (500 - server internal error)
                return responseError(HttpStatusCode.BadRequest, ex.toString())
            })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
