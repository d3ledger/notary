package com.d3.btc.withdrawal.transaction

import com.d3.btc.helper.address.createMsRedeemScript
import com.d3.btc.helper.address.getSignThreshold
import com.d3.btc.helper.address.outPutToBase58Address
import com.d3.btc.helper.address.toEcPubKey
import com.d3.commons.model.IrohaCredential
import com.d3.commons.notary.IrohaCommand
import com.d3.commons.notary.IrohaTransaction
import com.d3.commons.sidechain.iroha.BTC_SIGN_COLLECT_DOMAIN
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConverter
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.sidechain.iroha.util.getAccountDetails
import com.d3.commons.util.getRandomId
import com.d3.commons.util.hex
import com.d3.commons.util.irohaEscape
import com.d3.commons.util.unHex
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.QueryAPI
import mu.KLogging
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.ScriptBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/*
    Class that is used to collect signatures in Iroha
 */
@Component
class SignCollector(
    @Qualifier("signatureCollectorCredential")
    @Autowired private val signatureCollectorCredential: IrohaCredential,
    @Qualifier("signatureCollectorConsumer")
    @Autowired private val signatureCollectorConsumer: IrohaConsumer,
    @Autowired private val irohaAPI: IrohaAPI,
    @Autowired private val transactionSigner: TransactionSigner
) {
    private val queryAPI by lazy {
        QueryAPI(
            irohaAPI,
            signatureCollectorCredential.accountId,
            signatureCollectorCredential.keyPair
        )
    }
    //Adapter for JSON serialization/deserialization
    private val inputSignatureJsonAdapter = Moshi.Builder().build()
        .adapter<List<InputSignature>>(Types.newParameterizedType(List::class.java, InputSignature::class.java))

    /**
     * Collects current notary signatures. Process consists of 3 steps:
     * 1) Sign tx
     * 2) Create special account named after tx hash for signature storing
     * 3) Save signatures in recently created account details
     * @param tx - transaction to sign
     * @param walletPath - path to current wallet. Used to get private keys
     */
    fun collectSignatures(tx: Transaction, walletPath: String) {
        transactionSigner.sign(tx, walletPath).flatMap { signedInputs ->
            if (signedInputs.isEmpty()) {
                logger.warn(
                    "Cannot sign transaction ${tx.hashAsString}. " +
                            "Current node probably doesn't posses private keys for a given transaction to sign."
                )
                return@flatMap Result.of {}
            }
            logger.info { "Tx ${tx.hashAsString} signatures to add in Iroha $signedInputs" }
            val shortTxHash = shortTxHash(tx)
            val createAccountTx = IrohaConverter.convert(createSignCollectionAccountTx(shortTxHash))
            /**
             * We create a dedicated account on every withdrawal event.
             * We need this account to store transaction signatures from all the nodes.
             * Every node will try to create an account, but only one creation will succeed.
             * The following Iroha command can fail.
             */
            signatureCollectorConsumer.send(createAccountTx)
            val setSignaturesTx = IrohaConverter.convert(setSignatureDetailsTx(shortTxHash, signedInputs))
            signatureCollectorConsumer.send(setSignaturesTx)
        }.fold(
            {
                logger.info { "Signatures for ${tx.hashAsString} were successfully processed" }
            }, { ex -> throw ex })
    }

    /**
     * Returns signatures from all the nodes for a given transaction
     * @param txHash - transaction hash
     * @return result with map full of signatures. Format is <input index: list of signatures in hex format>
     */
    fun getSignatures(txHash: String): Result<Map<Int, List<SignaturePubKey>>, Exception> {
        /*
        Special account that is used to store given tx signatures.
        We use first 32 tx hash symbols as account name because of Iroha account name restrictions ([a-z_0-9]{1,32})
        */
        val signCollectionAccountId = "${shortTxHash(txHash)}@$BTC_SIGN_COLLECT_DOMAIN"
        return getAccountDetails(
            queryAPI,
            signCollectionAccountId,
            signatureCollectorCredential.accountId
        ).map { signatureDetails ->
            val totalInputSignatures = HashMap<Int, ArrayList<SignaturePubKey>>()
            signatureDetails.entries.forEach { signatureData ->
                val notaryInputSignatures = inputSignatureJsonAdapter.fromJson(signatureData.value)!!
                combineSignatures(totalInputSignatures, notaryInputSignatures)
            }
            totalInputSignatures
        }
    }

    /**
     * Checks if enough signatures for inputs were collected to execute withdrawal
     * @param tx - previously created unsigned transaction full of inputs
     * @param signatures - map full of input signatures from other notary nodes
     * @return true if all inputs are properly signed
     */
    fun isEnoughSignaturesCollected(tx: Transaction, signatures: Map<Int, List<SignaturePubKey>>): Boolean {
        var inputIndex = 0
        tx.inputs.forEach { input ->
            if (!signatures.containsKey(inputIndex)) {
                logger.info { "Tx ${tx.hashAsString} input at index $inputIndex is not signed yet" }
                return false
            }
            val inputAddress = outPutToBase58Address(input.connectedOutput!!)
            transactionSigner.getUsedPubKeys(inputAddress).fold(
                { usedPubKeys ->
                    val threshold = getSignThreshold(usedPubKeys)
                    val collectedSignatures = signatures[inputIndex]!!.size
                    if (collectedSignatures < threshold) {
                        logger.info { "Tx ${tx.hashAsString} input at index $inputIndex has $collectedSignatures signatures out of $threshold required " }
                        return false
                    }
                    inputIndex += 1
                }, { ex -> throw ex })
        }
        return true
    }

    /**
     * Fills given transaction with input signatures
     * @param tx - transaction to fill with signatures
     * @param signatures - map full of input signatures from other notary nodes
     */
    fun fillTxWithSignatures(
        tx: Transaction,
        signatures: Map<Int, List<SignaturePubKey>>
    ): Result<Unit, Exception> {
        return Result.of {
            var inputIndex = 0
            tx.inputs.forEach { input ->
                val inputAddress = outPutToBase58Address(input.connectedOutput!!)
                transactionSigner.getUsedPubKeys(inputAddress).fold({ usedKeys ->
                    /**
                     * Signatures must be ordered the same way public keys are ordered in redeem script
                     */
                    val orderedSignatures = signatures[inputIndex]!!.sortedWith(Comparator { sig1, sig2 ->
                        ECKey.PUBKEY_COMPARATOR.compare(
                            toEcPubKey(sig1.pubKey), toEcPubKey(sig2.pubKey)
                        )
                    })
                    val redeemScript = createMsRedeemScript(usedKeys)
                    logger.info("Redeem script for tx ${tx.hashAsString} input $inputIndex is $redeemScript")
                    logger.info("Signatures for tx ${tx.hashAsString}\n $orderedSignatures\nUsed keys $usedKeys")
                    val inputScript = ScriptBuilder.createP2SHMultiSigInputScript(
                        orderedSignatures.map { signature ->
                            decodeSignatureFromHex(signature.signatureHex)
                        },
                        redeemScript
                    )
                    input.scriptSig = inputScript
                    input.verify()
                }, { ex ->
                    throw IllegalStateException("Cannot get used keys", ex)
                })
                inputIndex++
            }
        }
    }

    //Decodes hex into signature object
    private fun decodeSignatureFromHex(signatureHex: String): TransactionSignature {
        return TransactionSignature(
            ECKey.ECDSASignature.decodeFromDER(String.unHex(signatureHex)),
            Transaction.SigHash.ALL,
            false
        )
    }

    /**
     * Function that combines signatures from Iroha into map.
     * @param totalInputSignatures - collection that stores all the signatures in convenient form: input index as key and list of signatures as value
     * @param notaryInputSignatures - signatures of particular node from Iroha. It will be added to [totalInputSignatures]
     */
    private fun combineSignatures(
        totalInputSignatures: HashMap<Int, ArrayList<SignaturePubKey>>,
        notaryInputSignatures: List<InputSignature>
    ) {
        notaryInputSignatures.forEach { inputSignature ->
            if (totalInputSignatures.containsKey(inputSignature.index)) {
                totalInputSignatures[inputSignature.index]!!.add(inputSignature.sigPubKey)
            } else {
                totalInputSignatures[inputSignature.index] = ArrayList(listOf(inputSignature.sigPubKey))
            }
        }
    }

    //Cuts tx hash using raw tx hash string. Only first 32 symbols are taken.
    fun shortTxHash(txHash: String) = txHash.substring(0, 32)

    //Cuts tx hash using tx
    fun shortTxHash(tx: Transaction) = shortTxHash(tx.hashAsString)

    //Creates Iroha transaction to create signature storing account
    private fun createSignCollectionAccountTx(txShortHash: String): IrohaTransaction {
        return IrohaTransaction(
            signatureCollectorCredential.accountId,
            ModelUtil.getCurrentTime(),
            1,
            arrayListOf(
                IrohaCommand.CommandCreateAccount(
                    txShortHash,
                    BTC_SIGN_COLLECT_DOMAIN,
                    String.hex(signatureCollectorCredential.keyPair.public.encoded)
                )
            )
        )
    }

    //Creates Iroha transaction to store signatures as acount details
    private fun setSignatureDetailsTx(txShortHash: String, signedInputs: List<InputSignature>): IrohaTransaction {
        val signCollectionAccountId = "$txShortHash@$BTC_SIGN_COLLECT_DOMAIN"
        val signaturesJson = inputSignatureJsonAdapter.toJson(signedInputs).irohaEscape()
        return IrohaTransaction(
            signatureCollectorCredential.accountId,
            ModelUtil.getCurrentTime(),
            1,
            arrayListOf(
                IrohaCommand.CommandSetAccountDetail(
                    signCollectionAccountId,
                    String.getRandomId(),
                    signaturesJson
                )
            )
        )
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
