package vendor

import mu.KotlinLogging
import org.junit.jupiter.api.Test

class LoggerUsageTest {
    @Test
    fun logUsage() {
        val log = KotlinLogging.logger { }
        log.info { "hello world" }
    }
}
