package com.d3.commons.registration

import com.d3.commons.sidechain.iroha.CLIENT_DOMAIN
import com.squareup.moshi.Moshi
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
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
    // Moshi adapter for response serialization
    val moshiAdapter = Moshi.Builder().build()!!.adapter(Map::class.java)!!


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
                    val pubkey = parameters["pubkey"]
                    val domain = parameters["domain"] ?: CLIENT_DOMAIN

                    val response = invokeRegistration(name, domain, pubkey)
                    call.respondText(response.message, status = response.code)
                }

                post("/users_new") {
                    val body = call.receive(UserDto::class)
                    val name = body.name
                    val pubkey = body.pubkey
                    val domain = body.domain ?: CLIENT_DOMAIN

                    val response = invokeRegistration(name, domain, pubkey)
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

    private fun invokeRegistration(
        name: String?,
        domain: String,
        pubkey: String?
    ): Response {
        logger.info { "Registration invoked with parameters (name = \"$name\", domain = \"$domain\", pubkey = \"$pubkey\"" }
        return onPostRegistration(name, domain, pubkey)
    }

    private fun responseError(code: HttpStatusCode, reason: String): Response {
        val errorMsg = "Response has been failed. $reason"
        logger.error { errorMsg }
        return Response(code, errorMsg)
    }

    private fun onPostRegistration(
        name: String?,
        domain: String?,
        pubkey: String?
    ): Response {
        var reason = ""
        if (name == null) reason = reason.plus("Parameter \"name\" is not specified. ")
        val clientDomain = domain ?: CLIENT_DOMAIN
        if (pubkey == null) reason = reason.plus("Parameter \"pubkey\" is not specified.")

        if (name == null || pubkey == null) {
            return responseError(HttpStatusCode.BadRequest, reason)
        }
        registrationStrategy.register(name, clientDomain, pubkey).fold(
            { address ->
                logger.info {
                    "Client $name@$clientDomain was successfully registered with address $address"
                }

                val response = mapOf("clientId" to address)
                val serialized = moshiAdapter.toJson(response)

                return Response(HttpStatusCode.OK, serialized)
            },
            { ex ->
                logger.error("Cannot register client $name", ex)

                val response = mapOf("message" to ex.message, "details" to ex.toString())
                val serialized = moshiAdapter.toJson(response)

                return responseError(HttpStatusCode.InternalServerError, serialized)
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

data class UserDto(
    val name: String,
    val pubkey: String,
    val domain: String?
)
