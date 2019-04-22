package com.d3.btc.provider.generation

import com.d3.btc.helper.address.createMsAddress
import com.d3.btc.model.AddressInfo
import com.d3.btc.model.BtcAddressType
import com.d3.btc.provider.network.BtcNetworkConfigProvider
import com.d3.commons.provider.NotaryPeerListProvider
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.d3.commons.util.getRandomId
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import mu.KLogging
import org.bitcoinj.wallet.Wallet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 *  Bitcoin keys provider
 *  @param keysWallet - bitcoin wallet
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
    @Autowired private val keysWallet: Wallet,
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
     * @param onKeyCreated - function that will be called right after key creation
     * @return new public key created by notary
     */
    fun createKey(sessionAccountName: String, onKeyCreated: () -> Unit): Result<String, Exception> {
        // Generate new key from wallet
        val key = keysWallet.freshReceiveKey()
        onKeyCreated()
        val pubKey = key.publicKeyAsHex
        return ModelUtil.setAccountDetail(
            sessionConsumer,
            "$sessionAccountName@btcSession",
            String.getRandomId(),
            pubKey
        ).map {
            logger.info { "New key has been generated" }
            pubKey
        }
    }

    /**
     * Creates multisignature address if enough public keys are provided
     * @param notaryKeys - list of all notaries public keys
     * @param addressType - type of address to create
     * @param generationTime - time of address generation. Used in Iroha multisig
     * @param nodeId - node id
     * @param onMsAddressCreated - function that will be called right after MS address creation
     * @return Result of operation
     */
    fun checkAndCreateMultiSigAddress(
        notaryKeys: List<String>,
        addressType: BtcAddressType,
        generationTime: Long,
        nodeId: String,
        onMsAddressCreated: () -> Unit
    ): Result<Unit, Exception> {
        return multiSigConsumer.getConsumerQuorum().map { quorum ->
            val peers = notaryPeerListProvider.getPeerList().size
            if (peers == 0) {
                throw IllegalStateException("No peers to create btc multisignature address")
            } else if (notaryKeys.size != peers) {
                logger.info {
                    "Not enough keys are collected to generate a multisig address(${notaryKeys.size}" +
                            " out of $peers)"
                }
                return@map
            } else if (!hasMyKey(notaryKeys)) {
                logger.info { "Cannot be involved in address generation. No access to $notaryKeys." }
                return@map
            }
            val msAddress = createMsAddress(notaryKeys, btcNetworkConfigProvider.getConfig())
            if (keysWallet.isAddressWatched(msAddress)) {
                logger.info("Address $msAddress has been already created")
                return@map
            } else if (!keysWallet.addWatchedAddress(msAddress)) {
                throw IllegalStateException("BTC address $msAddress was not added to wallet")
            }
            onMsAddressCreated()
            logger.info("Address $msAddress was added to wallet. Used keys are ${notaryKeys}")
            val addressStorage =
                createAddressStorage(addressType, notaryKeys, nodeId, generationTime)
            ModelUtil.setAccountDetail(
                multiSigConsumer,
                addressStorage.storageAccount,
                msAddress.toBase58(),
                addressStorage.addressInfo.toJson(),
                generationTime,
                quorum
            ).fold({
                logger.info { "New BTC ${addressType.title} address $msAddress was created. Node id '$nodeId'" }
            }, { ex -> throw Exception("Cannot create Bitcoin multisig address", ex) })
        }
    }

    /**
     * Checks if current notary has its key in notaryKeys
     * @param notaryKeys - public keys of notaries
     * @return true if at least one current notary key is among given notaryKeys
     */
    private fun hasMyKey(notaryKeys: Collection<String>) = notaryKeys.find { key ->
        keysWallet.issuedReceiveKeys.find { ecKey -> ecKey.publicKeyAsHex == key } != null
    } != null


    /**
     * Creates address storage object that depends on generated address type
     * @param addressType - type of address to generate
     * @param notaryKeys - keys that were used to generate this address
     * @param nodeId - node id
     * @param generationTime - time of address generation
     */
    private fun createAddressStorage(
        addressType: BtcAddressType,
        notaryKeys: Collection<String>,
        nodeId: String,
        generationTime: Long
    ): AddressStorage {
        val (addressInfo, storageAccount) = when (addressType) {
            BtcAddressType.CHANGE -> {
                logger.info { "Creating change address" }
                Pair(
                    AddressInfo.createChangeAddressInfo(
                        ArrayList<String>(notaryKeys),
                        nodeId,
                        generationTime
                    ),
                    changeAddressStorageAccount
                )
            }
            BtcAddressType.FREE -> {
                logger.info { "Creating free address" }
                Pair(
                    AddressInfo.createFreeAddressInfo(
                        ArrayList<String>(notaryKeys),
                        nodeId,
                        generationTime
                    ),
                    //TODO use another account to store addresses
                    notaryAccount
                )
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
