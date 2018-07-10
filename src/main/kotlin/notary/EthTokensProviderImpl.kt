package notary

import com.github.kittinunf.result.Result
import config.ConfigKeys
import notary.db.tables.Tokens
import org.jooq.impl.DSL
import java.sql.DriverManager

/** Implementation of [EthTokensProvider] with PostgreSQL storage. */
class EthTokensProviderImpl : EthTokensProvider {

    override fun getTokens(): Result<Map<String, String>, Exception> {
        return Result.of {
            val result = mutableMapOf<String, String>()

            val connection = DriverManager.getConnection(
                CONFIG[ConfigKeys.dbUrl],
                CONFIG[ConfigKeys.dbUsername],
                CONFIG[ConfigKeys.dbPassword]
            )

            DSL.using(connection).use { ctx ->
                val tokens = Tokens.TOKENS

                ctx.select(tokens.WALLET, tokens.TOKEN)
                    .from(tokens)
                    .forEach { (wallet, token) ->
                        result.put(wallet, token)
                    }
            }

            connection.close()

            result
        }
    }
}
