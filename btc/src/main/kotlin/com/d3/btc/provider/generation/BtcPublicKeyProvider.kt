package com.d3.btc.provider.generation

import com.d3.btc.helper.address.getSignThreshold
import com.d3.btc.model.AddressInfo
import com.d3.btc.model.BtcAddressType
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.btc.wallet.WalletFile
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import org.bitcoinj.script.ScriptBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import provider.NotaryPeerListProvider
import sidechain.iroha.consumer.IrohaConsumer
import sidechain.iroha.util.ModelUtil
import util.getRandomId

/**
 *  Bitcoin keys provider
 *  @param walletFile - bitcoin wallet
 *  @param notaryPeerListProvider - provider to query all current notaries
 *  @param notaryAccount - Iroha account of notary service.
 *  Used to store free BTC addresses that can be registered by clients later
 *  @param changeAddressStorageAccount - Iroha account used to store change addresses
 *  @param multiSigConsumer - consumer of multisignature Iroha account. Used to create multisignature transactions.
 *  @param sessionConsumer - consumer of session Iroha account. Used to store session data.
 *  @param btcNetworkConfigProvider - provider of network configuration
 */
@Component
class BtcPublicKeyProvider(
    @Autowired private val walletFile: WalletFile,
    @Autowired private val notaryPeerListProvider: NotaryPeerListProvider,
    @Qualifier("notaryAccount")
    @Autowired private val notaryAccount: String,
    @Qualifier("changeAddressStorageAccount")
    @Autowired private val changeAddressStorageAccount: String,
    @Qualifier("multiSigConsumer")
    @Autowired private val multiSigConsumer: IrohaConsumer,
    @Qualifier("sessionConsumer")
    @Autowired private val sessionConsumer: IrohaConsumer,
    @Autowired private val btcNetworkConfigProvider: BtcNetworkConfigProvider
) {
    init {
        logger.info { "BtcPublicKeyProvider was successfully initialized" }
    }

    /**
     * Creates notary public key and sets it into session account details
     * @param sessionAccountName - name of session account
     * @return new public key created by notary
     */
    fun createKey(sessionAccountName: String): Result<String, Exception> {
        // Generate new key from wallet
        val key = walletFile.wallet.freshReceiveKey()
        val pubKey = key.publicKeyAsHex
        return ModelUtil.setAccountDetail(
            sessionConsumer,
            "$sessionAccountName@btcSession",
            String.getRandomId(),
            pubKey
        ).map {
            logger.info { "New key has been generated" }
            walletFile.save()
            pubKey
        }
    }

    /**
     * Creates multisignature address if enough public keys are provided
     * @param notaryKeys - list of all notaries public keys
     * @param addressType - type of address to create
     * @param generationTime - time of address generation. Used in Iroha multisig
     * @param nodeId - node id
     * @return Result of operation
     */
    fun checkAndCreateMultiSigAddress(
        notaryKeys: Collection<String>,
        addressType: BtcAddressType,
        generationTime: Long,
        nodeId: String
    ): Result<Unit, Exception> {
        return Result.of {
            val peers = notaryPeerListProvider.getPeerList().size
            if (peers == 0) {
                throw IllegalStateException("No peers to create btc multisignature address")
            } else if (notaryKeys.size != peers) {
                logger.info {
                    "Not enough keys are collected to generate a multisig address(${notaryKeys.size}" +
                            " out of $peers)"
                }
                return@of
            } else if (!hasMyKey(notaryKeys)) {
                logger.info { "Cannot be involved in address generation. No access to $notaryKeys." }
                return@of
            }
            val threshold = getSignThreshold(peers)
            val msAddress = createMsAddress(notaryKeys, threshold)
            if (walletFile.wallet.isAddressWatched(msAddress)) {
                logger.info("Address $msAddress has been already created")
                return@of
            } else if (!walletFile.wallet.addWatchedAddress(msAddress)) {
                throw IllegalStateException("BTC address $msAddress was not added to wallet")
            }
            logger.info { "Address $msAddress was added to wallet." }
            val addressStorage = createAddressStorage(addressType, notaryKeys, nodeId)
            ModelUtil.setAccountDetail(
                multiSigConsumer,
                addressStorage.storageAccount,
                msAddress.toBase58(),
                addressStorage.addressInfo.toJson(),
                generationTime,
                quorum = peers
            ).fold({
                walletFile.save()
                logger.info { "New BTC ${addressType.title} address $msAddress was created. Node id '$nodeId'" }
            }, { ex -> throw ex })
        }
    }

    /**
     * Checks if current notary has its key in notaryKeys
     * @param notaryKeys - public keys of notaries
     * @return true if at least one current notary key is among given notaryKeys
     */
    private fun hasMyKey(notaryKeys: Collection<String>) = notaryKeys.find { key ->
        walletFile.wallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == key } != null
    } != null

    /**
     * Creates multi signature bitcoin address
     * @param notaryKeys - public keys of notaries
     * @return created address
     */
    private fun createMsAddress(notaryKeys: Collection<String>, threshold: Int): Address {
        val keys = ArrayList<ECKey>()
        notaryKeys.forEach { key ->
            val ecKey = ECKey.fromPublicOnly(Utils.parseAsHexOrBase58(key))
            keys.add(ecKey)
        }
        val script = ScriptBuilder.createP2SHOutputScript(threshold, keys)
        logger.info { "New BTC multisignature script $script" }
        return script.getToAddress(btcNetworkConfigProvider.getConfig())
    }


    /**
     * Creates address storage object that depends on generated address type
     * @param addressType - type of address to generate
     * @param notaryKeys - keys that were used to generate this address
     * @param nodeId - node id
     */
    private fun createAddressStorage(
        addressType: BtcAddressType,
        notaryKeys: Collection<String>,
        nodeId: String
    ): AddressStorage {
        val (addressInfo, storageAccount) = when (addressType) {
            BtcAddressType.CHANGE -> {
                logger.info { "Creating change address" }
                Pair(
                    AddressInfo.createChangeAddressInfo(ArrayList<String>(notaryKeys), nodeId),
                    changeAddressStorageAccount
                )
            }
            BtcAddressType.FREE -> {
                logger.info { "Creating free address" }
                Pair(AddressInfo.createFreeAddressInfo(ArrayList<String>(notaryKeys), nodeId), notaryAccount)
            }
        }
        return AddressStorage(addressInfo, storageAccount)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}

/**
 * Data class that holds information about account storage
 * @param addressInfo - stores information about address: used public keys, client name and etc
 * @param storageAccount - account where this information will be stored
 */
private data class AddressStorage(val addressInfo: AddressInfo, val storageAccount: String)
