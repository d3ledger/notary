package endpoint

import notary.NotaryStub
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import main.Configs
import mu.KLogging

/**
 * Class is waiting for custodian's intention for rollback
 */
class RefundEndpoint {

    init {
        val server = embeddedServer(Netty, port = Configs.refundPort) {
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