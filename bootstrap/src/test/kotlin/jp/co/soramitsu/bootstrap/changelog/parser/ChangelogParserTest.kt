package jp.co.soramitsu.bootstrap.changelog.parser

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChangelogParserTest {

    private val parser = ChangelogParser()

    /**
     * @given empty script
     * @when parse() is called
     * @then IllegalArgumentException is thrown
     */
    @Test(expected = IllegalArgumentException::class)
    fun testEmptyScript() {
        parser.parse("")
    }

    /**
     * @given invalid script
     * @when parse() is called
     * @then IllegalArgumentException is thrown
     */
    @Test(expected = IllegalArgumentException::class)
    fun testInvalidScript() {
        parser.parse("{")
    }

    /**
     * @given wrong interface script
     * @when parse() is called
     * @then IllegalArgumentException is thrown
     */
    @Test(expected = IllegalArgumentException::class)
    fun testWrongInterfaceScript() {
        val script = """

class BadInterface implements Runnable {
    @Override
    void run() {}
}
        """.trimIndent()
        parser.parse(script)
    }

    /**
     * @given valid script
     * @when parse() is called
     * @then script is executed normally
     */
    @Test
    fun testValidScript() {
        val script = """
import jp.co.soramitsu.bootstrap.changelog.ChangelogInterface
import jp.co.soramitsu.bootstrap.dto.AccountPublicInfo
import jp.co.soramitsu.bootstrap.dto.Peer
import jp.co.soramitsu.iroha.java.Transaction
import org.jetbrains.annotations.NotNull

class TestChangeLog implements ChangelogInterface {

    @Override
    List<Transaction> createChangelog(@NotNull List<AccountPublicInfo> accounts,
                                      @NotNull List<Peer> peers) {
        return []
    }
}
        """.trimIndent()
        assertTrue(parser.parse(script).createChangelog(emptyList(), emptyList()).isEmpty())
    }
}
