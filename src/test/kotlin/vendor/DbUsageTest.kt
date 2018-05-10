package vendor

import org.jooq.impl.DSL
import org.junit.jupiter.api.Test
import java.util.*
import org.junit.jupiter.api.Assertions.assertThrows

class DbUsageTest {

    @Test
    fun dbEmpty() {
        val properties = Properties()

        assertThrows(org.jooq.exception.DataAccessException::class.java, {
            DSL.using(
                properties.getProperty("db.url"),
                properties.getProperty("db.username"),
                properties.getProperty("db.password")
            ).use {}
        })

    }
}
