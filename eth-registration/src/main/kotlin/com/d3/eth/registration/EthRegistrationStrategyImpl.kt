package com.d3.eth.registration

import com.d3.commons.config.EthereumPasswords
import com.d3.commons.registration.IrohaEthAccountRegistrator
import com.d3.commons.registration.RegistrationStrategy
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.util.createPrettyScheduledThreadPool
import com.d3.eth.provider.EthFreeRelayProvider
import com.d3.eth.provider.EthRelayProvider
import com.d3.eth.sidechain.util.BasicAuthenticator
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import contract.RelayRegistry
import mu.KLogging
import okhttp3.OkHttpClient
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.JsonRpc2_0Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger

/**
 * Effective implementation of [RegistrationStrategy]
 */
class EthRegistrationStrategyImpl(
    private val ethFreeRelayProvider: EthFreeRelayProvider,
    private val ethRelayProvider: EthRelayProvider,
    ethRegistrationConfig: EthRegistrationConfig,
    passwordConfig: EthereumPasswords,
    private val irohaConsumer: IrohaConsumer,
    private val notaryIrohaAccount: String
) : RegistrationStrategy {

    init {
        logger.info { "Init EthRegistrationStrategyImpl with irohaCreator=${irohaConsumer.creator}, notaryIrohaAccount=$notaryIrohaAccount" }
    }

    private val ethereumAccountRegistrator =
        IrohaEthAccountRegistrator(irohaConsumer, notaryIrohaAccount)

    private val credentials = WalletUtils.loadCredentials(
        passwordConfig.credentialsPassword,
        ethRegistrationConfig.ethereum.credentialsPath
    )!!
    private val builder = OkHttpClient().newBuilder().authenticator(
        BasicAuthenticator(
            passwordConfig
        )
    )!!
    private val web3 = Web3j.build(
        HttpService(ethRegistrationConfig.ethereum.url, builder.build(), false),
        JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME.toLong(),
        createPrettyScheduledThreadPool(ETH_REGISTRATION_SERVICE_NAME, "web3j")
    )

    private val relayRegistry = RelayRegistry.load(
        ethRegistrationConfig.ethRelayRegistryAddress,
        web3,
        credentials,
        StaticGasProvider(
            BigInteger.valueOf(ethRegistrationConfig.ethereum.gasPrice),
            BigInteger.valueOf(ethRegistrationConfig.ethereum.gasLimit)
        )
    )

    /**
     * Register new notary client
     * @param accountName - client name
     * @param domainId - client domain
     * @param publicKey - client public key
     * @param whitelist - list of addresses from client
     * @return ethereum wallet has been registered
     */
    override fun register(
        accountName: String,
        domainId: String,
        whitelist: List<String>,
        publicKey: String
    ): Result<String, Exception> {
        return ethFreeRelayProvider.getRelay()
            .flatMap { freeEthWallet ->
                ethRelayProvider.getRelayByAccountId("$accountName@$domainId")
                    .flatMap { assignedRelays ->
                        // check that client hasn't been registered yet
                        if (assignedRelays.isPresent())
                            throw IllegalArgumentException("Client $accountName@$domainId has already been registered with relay: ${assignedRelays.get()}")

                        // register to Ethereum RelayRegistry
                        logger.info { "Add new relay to relay registry relayRegistry=$relayRegistry, freeWallet=$freeEthWallet, whitelist=$whitelist, creator=${credentials.address}." }
                        relayRegistry.addNewRelayAddress(freeEthWallet, whitelist).send()

                        // register relay to Iroha
                        ethereumAccountRegistrator.register(
                            freeEthWallet,
                            whitelist,
                            accountName,
                            domainId,
                            publicKey
                        )
                    }
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
