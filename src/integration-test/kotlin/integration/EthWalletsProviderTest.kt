package integration

import config.TestConfig
import config.loadConfigs
import notary.db.tables.Wallets
import org.jooq.impl.DSL
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.sql.DriverManager

/**
 * Test Postgres ethereum wallets provider
 */
class EthWalletsProviderTest {

    val dbConfig = loadConfigs("test", TestConfig::class.java, "/test.properties").db

    /**
     * List all wallets in database
     */
    @Disabled
    @Test
    fun listWallets() {
        val connection = DriverManager.getConnection(
            dbConfig.url,
            dbConfig.username,
            dbConfig.password
        )

        DSL.using(connection).use { ctx ->
            val wallets = Wallets.WALLETS

            // read
            ctx.select(wallets.WALLET, wallets.IROHAUSERNAME)
                .from(wallets)
                .forEach {
                    println(it)
                }
        }
    }
}
