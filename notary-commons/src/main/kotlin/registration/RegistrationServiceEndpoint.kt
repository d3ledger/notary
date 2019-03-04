package registration

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KLogging
import sidechain.iroha.CLIENT_DOMAIN

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
            install(ContentNegotiation) {
                gson()
            }
            routing {
                post("/users") {
                    val parameters = call.receiveParameters()
                    val name = parameters["name"]
                    val whitelist = parameters["whitelist"].toString().split(",")
                    val pubkey = parameters["pubkey"]
                    val domain = parameters["domain"]

                    logger.info { "Registration invoked with parameters (name = \"$name\", whitelist = \"$whitelist\", pubkey = \"$pubkey\"" }

                    val response = onPostRegistration(name, domain, whitelist, pubkey)
                    call.respondText(response.message, status = response.code)
                }

                get("free-addresses/number") {
                    val response = onGetFreeAddressesNumber()
                    call.respondText(response.message, status = response.code)
                }

                get("/actuator/health") {
                    call.respond(
                        mapOf(
                            "status" to "UP"
                        )
                    )
                }
            }
        }
        server.start(wait = false)
    }

    private fun responseError(code: HttpStatusCode, reason: String): Response {
        val errorMsg = "Response has been failed. $reason"
        logger.error { errorMsg }
        return Response(code, errorMsg)
    }

    private fun onPostRegistration(
        name: String?,
        domain: String?,
        whitelist: List<String>?,
        pubkey: String?
    ): Response {
        var reason = ""
        if (name == null) reason = reason.plus("Parameter \"name\" is not specified. ")
        val clientDomain = domain ?: CLIENT_DOMAIN
        if (whitelist == null) reason = reason.plus("Parameter \"whitelist\" is not specified. ")
        if (pubkey == null) reason = reason.plus("Parameter \"pubkey\" is not specified.")

        if (name == null || whitelist == null || pubkey == null) {
            return responseError(HttpStatusCode.BadRequest, reason)
        }
        registrationStrategy.register(name, clientDomain, whitelist.filter { it.isNotEmpty() }, pubkey).fold(
            { address ->
                logger.info {
                    "Client $name@$clientDomain was successfully registered with address $address"
                }
                return Response(HttpStatusCode.OK, address)
            },
            { ex ->
                logger.error("Cannot register client $name", ex)
                return responseError(HttpStatusCode.InternalServerError, ex.toString())
            })
    }

    private fun onGetFreeAddressesNumber(): Response {
        return registrationStrategy.getFreeAddressNumber()
            .fold(
                {
                    Response(HttpStatusCode.OK, it.toString())
                },
                {
                    responseError(HttpStatusCode.InternalServerError, it.toString())
                }
            )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
