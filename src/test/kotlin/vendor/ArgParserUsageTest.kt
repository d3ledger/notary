package vendor

import com.xenomachina.argparser.ArgParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArgParserUsageTest {

    class TestArgs(parser: ArgParser) {
        val v by parser.flagging("v option")
        val name by parser.storing("-N", "--name", help = "name option")
        val c by parser.storing("c option")
    }

    private val args = arrayOf("-c3", "-v", "-N", "\"Lev Tolstoy\"", "-c", "3")

    @Test
    fun argParserUsage() {
        ArgParser(args).parseInto(ArgParserUsageTest::TestArgs).run {
            assert(v)
            assertEquals("\"Lev Tolstoy\"", name)
            assertEquals("3", c)
        }
    }
}
