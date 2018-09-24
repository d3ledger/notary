package vendor

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.junit.jupiter.api.Test

/**
 * Starts a server for 15s.
 */
class KtorUsageTest {

    @Test
    fun ktorCompilationTest() {
        val server = embeddedServer(Netty, port = 8888) {
            routing {
                get("/") {
                    call.respondText("Hello World!", ContentType.Text.Plain)
                }
                get("/demo") {
                    call.respondText("HELLO WORLD!")
                }
            }
        }
        server.start(wait = false)
    }
}
