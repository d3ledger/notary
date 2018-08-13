package integration

import config.TestConfig
import config.loadConfigs
import notary.db.tables.Tokens
import org.jooq.impl.DSL
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.sql.DriverManager

/**
 * Test Postgres ethereum ERC20 tokens provider
 */
class EthTokensProviderTest {

    private val dbConfig = loadConfigs("test", TestConfig::class.java).db

    /**
     * List all tokens in database
     */
    @Disabled
    @Test
    fun listTokens() {
        val connection = DriverManager.getConnection(
            dbConfig.url,
            dbConfig.username,
            dbConfig.password
        )

        DSL.using(connection).use { ctx ->
            val tokens = Tokens.TOKENS

            // read
            ctx.select(tokens.WALLET, tokens.TOKEN)
                .from(tokens)
                .forEach {
                    println(it)
                }
        }
    }
}
