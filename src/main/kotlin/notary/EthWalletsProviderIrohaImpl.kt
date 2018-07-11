package notary

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.result.Result
import config.ConfigKeys
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger

/** Implementation of [EthWalletsProvider] with Iroha storage. */
class EthWalletsProviderIrohaImpl : EthWalletsProvider {

    private val relayRegistrationAccount = CONFIG[ConfigKeys.registrationServiceIrohaAccount]

    override fun getWallets(): Result<Map<String, String>, Exception> {
        return Result.of {

            val keypair = ModelUtil.loadKeypair(
                CONFIG[ConfigKeys.notaryPubkeyPath],
                CONFIG[ConfigKeys.notaryPrivkeyPath]
            ).get()

            val query = ModelUtil.getModelQueryBuilder()
                .creatorAccountId(CONFIG[ConfigKeys.notaryIrohaAccount])
                .createdTime(ModelUtil.getCurrentTime())
                .queryCounter(BigInteger.ONE)
                .getAccount(CONFIG[ConfigKeys.registrationServiceNotaryIrohaAccount])
                .build()

            val proto_query = ModelUtil.prepareQuery(query, keypair)

            val response = ModelUtil.getQueryStub().find(proto_query)

            val account = response.accountResponse.account
            val stringBuilder = StringBuilder(account.jsonData)
            val json: JsonObject = Parser().parse(stringBuilder) as JsonObject

            if (json.map[relayRegistrationAccount] == null)
                mapOf<String, String>()
            else {
                val wallets = json.map[relayRegistrationAccount] as Map<String, String>
                wallets.filterValues { it != "free" }
            }
        }
    }
}
