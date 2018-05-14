package endpoint

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import main.CONFIG
import main.ConfigKeys
import mu.KLogging
import notary.NotaryStub

/**
 * Class is waiting for custodian's intention for rollback
 */
class RefundEndpoint {

    init {
        val server = embeddedServer(Netty, port = CONFIG[ConfigKeys.refundPort]) {
            routing {
                get("/rollback") {
                    NotaryStub.logger.info { "Refund" }
                    call.respondText("done", ContentType.Text.Plain)
                }
            }
        }
        server.start(wait = false)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}