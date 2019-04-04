package jp.co.soramitsu.bootstrap.changelog.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
import jp.co.soramitsu.bootstrap.changelog.ChangelogAccountPublicInfo
import jp.co.soramitsu.bootstrap.changelog.ChangelogPeer
import jp.co.soramitsu.iroha.java.Transaction
import org.jetbrains.annotations.NotNull

class TestChangeLog implements ChangelogInterface {

    @Override
    String getSchemaVersion() {
        return "1.0"
    }

    @Override
    Transaction createChangelog(@NotNull List<ChangelogAccountPublicInfo> accounts,
                                      @NotNull List<ChangelogPeer> peers) {
        return null
    }
}
        """.trimIndent()
        val changelog = parser.parse(script)
        assertNull(changelog.createChangelog(emptyList(), emptyList()))
        assertEquals("1.0", changelog.schemaVersion)
    }
}
