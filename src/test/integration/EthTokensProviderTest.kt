package integration

import main.CONFIG
import main.ConfigKeys
import notary.db.tables.Tokens
import org.jooq.impl.DSL
import java.sql.DriverManager
import org.junit.jupiter.api.Test

/**
 * Test Postgres ethereum ERC20 tokens provider
 */
class EthTokensProviderTest {

    /**
     * List all tokens in database
     */
    //    @Test
    fun listTokens() {
        val connection = DriverManager.getConnection(
            CONFIG[ConfigKeys.dbUrl],
            CONFIG[ConfigKeys.dbUsername],
            CONFIG[ConfigKeys.dbPassword]
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
