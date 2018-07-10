package notary

import com.github.kittinunf.result.Result
import config.ConfigKeys
import notary.db.tables.Wallets
import org.jooq.impl.DSL
import java.sql.DriverManager

/** Implementation of [EthWalletsProvider] with PostgreSQL storage. */
class EthWalletsProviderPostgresImpl : EthWalletsProvider {

    override fun getWallets(): Result<Map<String, String>, Exception> {
        return Result.of {
            val result = mutableMapOf<String, String>()

            val connection = DriverManager.getConnection(
                CONFIG[ConfigKeys.dbUrl],
                CONFIG[ConfigKeys.dbUsername],
                CONFIG[ConfigKeys.dbPassword]
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
