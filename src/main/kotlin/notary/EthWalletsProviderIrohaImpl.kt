package notary

import com.github.kittinunf.result.Result
import main.ConfigKeys
import sidechain.iroha.util.ModelUtil
import java.math.BigInteger


/** Implementation of [EthWalletsProvider] with Iroha storage. */
class EthWalletsProviderIrohaImpl : EthWalletsProvider {

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

            ModelUtil.jsonToKV(response.accountResponse.account.jsonData)!!
                .filter { it.key == CONFIG[ConfigKeys.registrationServiceIrohaAccount] }
                .values
                .first()
                .filter {
                    it.value != "free"
                }
        }
    }
}
