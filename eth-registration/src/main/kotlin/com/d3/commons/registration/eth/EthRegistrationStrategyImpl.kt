package com.d3.commons.registration.eth

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.d3.commons.config.EthereumPasswords
import contract.RelayRegistry
import mu.KLogging
import okhttp3.OkHttpClient
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import com.d3.commons.provider.eth.EthFreeRelayProvider
import com.d3.commons.registration.IrohaEthAccountCreator
import com.d3.commons.registration.RegistrationStrategy
import com.d3.commons.sidechain.eth.util.BasicAuthenticator
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import java.math.BigInteger

/**
 * Effective implementation of [RegistrationStrategy]
 */
class EthRegistrationStrategyImpl(
    private val ethFreeRelayProvider: EthFreeRelayProvider,
    ethRegistrationConfig: EthRegistrationConfig,
    passwordConfig: EthereumPasswords,
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String
) : RegistrationStrategy {

    init {
        logger.info { "Init EthRegistrationStrategyImpl with irohaCreator=${irohaConsumer.creator}, notaryIrohaAccount=$notaryIrohaAccount" }
    }

    private val credentials = WalletUtils.loadCredentials(
        passwordConfig.credentialsPassword,
        ethRegistrationConfig.ethereum.credentialsPath
    )!!
    private val builder = OkHttpClient().newBuilder().authenticator(BasicAuthenticator(passwordConfig))!!
    private val web3 = Web3j.build(HttpService(ethRegistrationConfig.ethereum.url, builder.build(), false))!!

    private val relayRegistry = RelayRegistry.load(
        ethRegistrationConfig.ethRelayRegistryAddress,
        web3,
        credentials,
        BigInteger.valueOf(ethRegistrationConfig.ethereum.gasPrice),
        BigInteger.valueOf(ethRegistrationConfig.ethereum.gasLimit)
    )

    private val irohaEthAccountCreator = IrohaEthAccountCreator(irohaConsumer, notaryIrohaAccount)

    /**
     * Register new notary client
     * @param name - client name
     * @param pubkey - client public key
     * @param whitelist - list of addresses from client
     * @return ethereum wallet has been registered
     */
    override fun register(
        name: String,
        domain: String,
        whitelist: List<String>,
        pubkey: String
    ): Result<String, Exception> {
        return ethFreeRelayProvider.getRelay()
            .flatMap { freeEthWallet ->
                logger.info { "Add new relay to relay registry relayRegistry=$relayRegistry, freeWallet=$freeEthWallet, whitelist=$whitelist, creator=${credentials.address}." }
                relayRegistry.addNewRelayAddress(freeEthWallet, whitelist).send()
                irohaEthAccountCreator.create(
                    freeEthWallet, whitelist, name, domain, pubkey
                )
            }
    }

    /**
     * Return number of free relays.
     */
    override fun getFreeAddressNumber(): Result<Int, Exception> {
        return ethFreeRelayProvider.getRelays()
            .map { freeRelays ->
                freeRelays.size
            }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
