package notary

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.result.Result
import jp.co.soramitsu.iroha.Keypair
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger

/**
 * Implementation of [EthWalletsProvider] with Iroha storage.
 *
 * @param creator - Iroha query creator
 * @param keypair - Iroha keypair to query
 * @param relayRegistrationAccount - account of a registration service that has set details
 * @param registrationServiceNotaryIrohaAccount - notary account that contains details
 */
class EthWalletsProviderIrohaImpl(
    val creator: String,
    val keypair: Keypair,
    val relayRegistrationAccount: String,
    val registrationServiceNotaryIrohaAccount: String
) : EthWalletsProvider {

    /**
     * Account of registration service that has set details.
     *
     * @return map<eth_wallet -> iroha_account> in success case or exception otherwise
     */
    override fun getWallets(): Result<Map<String, String>, Exception> {
        return Result.of {
            val query = ModelUtil.getModelQueryBuilder()
                .creatorAccountId(creator)
                .createdTime(ModelUtil.getCurrentTime())
                .queryCounter(BigInteger.ONE)
                .getAccount(registrationServiceNotaryIrohaAccount)
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
