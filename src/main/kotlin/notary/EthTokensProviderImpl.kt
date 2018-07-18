package notary

import com.github.kittinunf.result.Result
import config.DatabaseConfig
import notary.db.tables.Tokens
import org.jooq.impl.DSL
import java.sql.DriverManager

/** Implementation of [EthTokensProvider] with PostgreSQL storage. */
class EthTokensProviderImpl(val dbConfig: DatabaseConfig) : EthTokensProvider {

    override fun getTokens(): Result<Map<String, String>, Exception> {
        return Result.of {
            val result = mutableMapOf<String, String>()

            val connection = DriverManager.getConnection(
                dbConfig.url,
                dbConfig.username,
                dbConfig.password
            )

            DSL.using(connection).use { ctx ->
                val tokens = Tokens.TOKENS

                ctx.select(tokens.WALLET, tokens.TOKEN)
                    .from(tokens)
                    .forEach { (wallet, token) ->
                        result[wallet] = token
                    }
            }

            connection.close()

            result
        }
    }
}
