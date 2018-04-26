package vendor

import com.xenomachina.argparser.ArgParser

import org.junit.Assert
import org.junit.Test

class ArgParserUsageTest {

    class TestArgs(parser: ArgParser) {
        val v by parser.flagging("v option")
        val name by parser.storing("-N", "--name", help = "name option")
        val c by parser.storing("c option")
    }

    val args = arrayOf("-c3", "-v", "-N", "\"Lev Tolstoy\"", "-c", "3")

    @Test
    fun argParserUsage() {
        ArgParser(args).parseInto(ArgParserUsageTest::TestArgs).run {
            Assert.assertTrue(v == true)
            Assert.assertEquals("\"Lev Tolstoy\"", name)
            Assert.assertEquals("3", c)
        }
    }
}