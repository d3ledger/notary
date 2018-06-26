package registration

import io.ktor.application.call
import io.ktor.request.receiveParameters
import io.ktor.response.respondText
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogging

/**
 * Registration HTTP service
 */
class RegistrationServiceEndpoint(
    port: Int,
    private val registrationStrategy: RegistrationStrategy
) {

    init {
        val server = embeddedServer(Netty, port = port) {
            routing {
                post("/register") {
                    val parameters = call.receiveParameters()
                    val name = parameters["name"]
                    val pubkey = parameters["pubkey"]

                    logger.info { "Registration invoked with parameters (name = \"$name\", pubkey = \"$pubkey\"" }

                    call.respondText { onPostRegistration(name, pubkey) }
                }
            }
        }
        server.start(wait = false)
    }

    private fun responseError(reason: String): String {
        logger.warn { "Response has been failed. $reason" }
        return "Response has been failed. $reason"
    }

    private fun onPostRegistration(name: String?, pubkey: String?): String {
        if (name == null)
            return responseError("Parameter \"name\" is not specified.")
        if (pubkey == null)
            return responseError("Parameter \"pubkey\" is not specified.")

        return registrationStrategy.register(name, pubkey).fold(
            {
                "OK"
            },
            {
                responseError(it.toString())
            })
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
