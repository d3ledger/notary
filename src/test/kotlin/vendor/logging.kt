package vendor

import mu.KotlinLogging
import org.junit.jupiter.api.Test

class LoggerUsage {
    @Test
    fun logUsage() {
        val log = KotlinLogging.logger { }
        log.info { "hello world" }
    }
}
