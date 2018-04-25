package vendor


import org.jooq.impl.DSL
import org.junit.Test
import java.util.*

class dbUsageTest {

    @Test(expected = org.jooq.exception.DataAccessException::class)
    fun dbEmpty() {
        val properties = Properties()

        DSL.using(
                properties.getProperty("db.url"),
                properties.getProperty("db.username"),
                properties.getProperty("db.password")
        ).use {}
    }
}
