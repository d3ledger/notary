package notary

import com.github.kittinunf.result.Result
import config.DatabaseConfig
import notary.db.tables.Wallets
import org.jooq.impl.DSL
import java.sql.DriverManager

/** Implementation of [EthRelayProvider] with PostgreSQL storage. */
class EthRelayProviderPostgresImpl(val databaseConfig: DatabaseConfig) : EthRelayProvider {

    override fun getRelays(): Result<Map<String, String>, Exception> {
        return Result.of {
            val result = mutableMapOf<String, String>()

            val connection = DriverManager.getConnection(
                databaseConfig.url,
                databaseConfig.username,
                databaseConfig.password
            )

            DSL.using(connection).use { ctx ->
                val wallets = Wallets.WALLETS

                ctx.select(wallets.WALLET, wallets.IROHAUSERNAME)
                    .from(wallets)
                    .forEach { (wallet, user) ->
                        result.put(wallet, user)
                    }
            }

            connection.close()

            result
        }
    }
}
