package withdrawal.transaction

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import model.IrohaCredential
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaTransaction
import org.bitcoinj.core.Transaction
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import sidechain.iroha.BTC_SIGN_COLLECT_DOMAIN
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.consumer.IrohaConverterImpl
import sidechain.iroha.consumer.IrohaNetwork
import sidechain.iroha.util.ModelUtil
import sidechain.iroha.util.getAccountDetails
import util.getRandomId
import util.irohaEscape

/*
    Class that is used to collect signatures in Iroha
 */
@Component
class SignCollector(
    @Autowired private val irohaNetwork: IrohaNetwork,
    @Autowired private val withdrawalCredential: IrohaCredential,
    @Autowired private val withdrawalConsumer: IrohaConsumer,
    @Autowired private val transactionSigner: TransactionSigner
) {

    //Adapter for JSON serialization/deserialization
    private val inputSignatureJsonAdapter = Moshi.Builder().build()
        .adapter<List<InputSignature>>(Types.newParameterizedType(List::class.java, InputSignature::class.java))

    /**
     * Adds current notary signatures to Iroha
     * @param tx - transaction to sign
     * @param wallet - current wallet. Used to get private keys
     */
    fun addSignatures(tx: Transaction, wallet: Wallet) {
        transactionSigner.sign(tx, wallet).flatMap { signedInputs ->
            if (signedInputs.isEmpty()) {
                throw IllegalStateException("No inputs were signed")
            }
            val shortTxHash = shortTxHash(tx)
            val createAccountTx = IrohaConverterImpl().convert(createSignCollectionAccountTx(shortTxHash))
            withdrawalConsumer.sendAndCheck(createAccountTx)
                .failure { ex -> logger.warn("Cannot create signature storing account for tx ${tx.hashAsString}", ex) }
            val setSignaturesTx = IrohaConverterImpl().convert(setSignatureDetailsTx(shortTxHash, signedInputs))
            withdrawalConsumer.sendAndCheck(setSignaturesTx)
        }.fold(
            {
                logger.info { "Signatures for ${tx.hashAsString} were successfully saved in Iroha" }
            }, { ex -> throw ex })
    }

    /**
     * Returns signatures from all the nodes for a given transaction
     * @param txHash - transaction hash
     * @return result with map full of signatures. Format is <input index: list of signatures in hex format>
     */
    fun getSignatures(txHash: String): Result<Map<Int, List<String>>, Exception> {
        /*
        Special account that is used to store given tx signatures.
        We use first 32 tx hash symbols as account name because of Iroha account name restrictions ([a-z_0-9]{1,32})
        */
        val signCollectionAccountId = "${shortTxHash(txHash)}@$BTC_SIGN_COLLECT_DOMAIN"
        return getAccountDetails(
            withdrawalCredential,
            irohaNetwork,
            signCollectionAccountId,
            withdrawalCredential.accountId
        ).map { signatureDetails ->
            val totalInputSignatures = HashMap<Int, ArrayList<String>>()
            signatureDetails.entries.forEach { signatureData ->
                val notaryInputSignatures = inputSignatureJsonAdapter.fromJson(signatureData.value)!!
                collectSignatures(totalInputSignatures, notaryInputSignatures)
            }
            totalInputSignatures
        }
    }

    //Function that combines signatures from Iroha into map.
    private fun collectSignatures(
        totalInputSignatures: HashMap<Int, ArrayList<String>>,
        notaryInputSignatures: List<InputSignature>
    ) {
        notaryInputSignatures.forEach { inputSignature ->
            if (totalInputSignatures.containsKey(inputSignature.index)) {
                totalInputSignatures[inputSignature.index]!!.add(inputSignature.signatureHex)
            } else {
                totalInputSignatures[inputSignature.index] = ArrayList(listOf(inputSignature.signatureHex))
            }
        }
    }

    //Cuts tx hash using raw tx hash string
    private fun shortTxHash(txHash: String) = txHash.substring(0, 32)

    //Cuts tx hash using tx
    private fun shortTxHash(tx: Transaction) = shortTxHash(tx.hashAsString)

    //Creates Iroha transaction to create signature storing account
    private fun createSignCollectionAccountTx(txShortHash: String): IrohaTransaction {
        return IrohaTransaction(
            withdrawalCredential.accountId,
            ModelUtil.getCurrentTime(),
            1,
            arrayListOf(
                IrohaCommand.CommandCreateAccount(
                    txShortHash,
                    BTC_SIGN_COLLECT_DOMAIN,
                    withdrawalCredential.keyPair.publicKey().hex()
                )
            )
        )
    }

    //Creates Iroha transaction to store signatures as acount details
    private fun setSignatureDetailsTx(txShortHash: String, signedInputs: List<InputSignature>): IrohaTransaction {
        val signCollectionAccountId = "$txShortHash@$BTC_SIGN_COLLECT_DOMAIN"
        return IrohaTransaction(
            withdrawalCredential.accountId,
            ModelUtil.getCurrentTime(),
            1,
            arrayListOf(
                IrohaCommand.CommandSetAccountDetail(
                    signCollectionAccountId,
                    String.getRandomId(),
                    String.irohaEscape(inputSignatureJsonAdapter.toJson(signedInputs))
                )
            )
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
