package integration.helper

import com.github.jleskovar.btcrpc.BitcoinRpcClientFactory
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.failure
import com.github.kittinunf.result.map
import config.BitcoinConfig
import helper.currency.satToBtc
import helper.network.getBlockChain
import mu.KLogging
import notary.IrohaCommand
import notary.IrohaOrderedBatch
import notary.IrohaTransaction
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.wallet.Wallet
import provider.btc.account.IrohaBtcAccountCreator
import provider.btc.address.AddressInfo
import provider.btc.address.BtcAddressesProvider
import provider.btc.address.BtcRegisteredAddressesProvider
import registration.btc.strategy.BtcRegistrationStrategyImpl
import sidechain.iroha.consumer.IrohaConsumerImpl
import sidechain.iroha.consumer.IrohaConverter
import sidechain.iroha.util.ModelUtil
import util.toHexString
import java.io.File
import java.math.BigDecimal
import java.security.KeyPair

const val BTC_ASSET = "btc#bitcoin"
private const val GENERATED_ADDRESSES_PER_BATCH = 5

class BtcIntegrationHelperUtil : IrohaIntegrationHelperUtil() {

    override val configHelper by lazy { BtcConfigHelper(accountHelper) }

    private val mstRegistrationIrohaConsumer by lazy {
        IrohaConsumerImpl(accountHelper.mstRegistrationAccount, irohaAPI)
    }

    private val rpcClient by lazy {
        BitcoinRpcClientFactory.createClient(
            user = "test",
            password = "test",
            host = BitcoinConfig.extractHosts(configHelper.createBtcNotaryConfig().bitcoin)[0],
            port = 8332,
            secure = false
        )
    }

    private val btcRegistrationStrategy by lazy {
        val btcAddressesProvider =
            BtcAddressesProvider(
                queryAPI,
                accountHelper.mstRegistrationAccount.accountId,
                accountHelper.notaryAccount.accountId
            )
        val btcTakenAddressesProvider =
            BtcRegisteredAddressesProvider(
                queryAPI,
                accountHelper.registrationAccount.accountId,
                accountHelper.notaryAccount.accountId
            )
        val irohaBtcAccountCreator = IrohaBtcAccountCreator(
            registrationConsumer,
            accountHelper.notaryAccount.accountId
        )
        BtcRegistrationStrategyImpl(
            btcAddressesProvider,
            btcTakenAddressesProvider,
            irohaBtcAccountCreator
        )
    }

    /**
     * Pregenerates multiple BTC addresses that can be registered later
     * @param walletFilePath - path to wallet file
     * @param addressesToGenerate - number of addresses to generate
     */
    fun preGenFreeBtcAddresses(walletFilePath: String, addressesToGenerate: Int) {
        val totalBatches = Math.ceil(addressesToGenerate.div(GENERATED_ADDRESSES_PER_BATCH.toDouble())).toInt()
        /*
         Iroha dies if it sees too much of transactions in a batch.
          */
        for (batch in 1..totalBatches) {
            if (batch == totalBatches) {
                preGenFreeBtcAddressesBatch(
                    walletFilePath,
                    addressesToGenerate - (totalBatches - 1) * GENERATED_ADDRESSES_PER_BATCH
                )
            } else {
                preGenFreeBtcAddressesBatch(walletFilePath, GENERATED_ADDRESSES_PER_BATCH)
            }
        }
    }

    /**
     * Creates and executes a batch full of generated BTC addresses
     * @param walletFilePath - path to wallet file
     * @param addressesToGenerate - number of addresses to generate
     */
    private fun preGenFreeBtcAddressesBatch(walletFilePath: String, addressesToGenerate: Int) {
        val irohaTxList = ArrayList<IrohaTransaction>()
        for (i in 1..addressesToGenerate) {
            val (key, address) = generateKeyAndAddress(walletFilePath)
            val irohaTx = IrohaTransaction(
                mstRegistrationIrohaConsumer.creator,
                ModelUtil.getCurrentTime(),
                1,
                arrayListOf(
                    IrohaCommand.CommandSetAccountDetail(
                        accountHelper.notaryAccount.accountId,
                        address.toBase58(),
                        AddressInfo.createFreeAddressInfo(listOf(key.publicKeyAsHex)).toJson()
                    )
                )
            )
            irohaTxList.add(irohaTx)
        }
        val utx = IrohaConverter.convert(IrohaOrderedBatch(irohaTxList))
        mstRegistrationIrohaConsumer.send(utx).failure { ex -> throw ex }
    }

    /**
     * Returns group of peers
     */
    fun getPeerGroup(wallet: Wallet, networkParameters: NetworkParameters, blockStoragePath: String): PeerGroup {
        return PeerGroup(networkParameters, getBlockChain(wallet, networkParameters, blockStoragePath))
    }

    private fun createMsAddress(keys: List<ECKey>): Address {
        val script = ScriptBuilder.createP2SHOutputScript(1, keys)
        return script.getToAddress(RegTestParams.get())
    }

    /**
     * Generates one BTC address that can be registered later
     * @param walletFilePath - path to wallet file
     * @return randomly generated BTC address
     */
    fun genFreeBtcAddress(walletFilePath: String): Result<Address, Exception> {
        val (key, address) = generateKeyAndAddress(walletFilePath)
        return ModelUtil.setAccountDetail(
            mstRegistrationIrohaConsumer,
            accountHelper.notaryAccount.accountId,
            address.toBase58(),
            AddressInfo.createFreeAddressInfo(listOf(key.publicKeyAsHex)).toJson()
        ).map { address }
    }

    /**
     * Generates one BTC address that can be registered later
     * @param walletFilePath - path to wallet file
     * @return randomly generated BTC address
     */
    fun genChangeBtcAddress(walletFilePath: String): Result<Address, Exception> {
        val (key, address) = generateKeyAndAddress(walletFilePath)
        return ModelUtil.setAccountDetail(
            mstRegistrationIrohaConsumer,
            accountHelper.changeAddressesStorageAccount.accountId,
            address.toBase58(),
            AddressInfo.createChangeAddressInfo(listOf(key.publicKeyAsHex)).toJson()
        ).map { address }
    }

    // Generates key and key based address
    private fun generateKeyAndAddress(walletFilePath: String): Pair<DeterministicKey, Address> {
        val walletFile = File(walletFilePath)
        val wallet = Wallet.loadFromFile(walletFile)
        val key = wallet.freshReceiveKey()
        val address = createMsAddress(listOf(key))
        wallet.addWatchedAddress(address)
        wallet.saveToFile(walletFile)
        logger.info { "generated address $address" }
        return Pair(key, address)
    }

    /**
     * Registers BTC client
     * @param walletFilePath - path to wallet file
     * @param irohaAccountName - client account in Iroha
     * @param keypair - key pair for new client in Iroha
     * @return btc address related to client
     */
    fun registerBtcAddress(
        walletFilePath: String,
        irohaAccountName: String,
        keypair: KeyPair = ModelUtil.generateKeypair()
    ): String {
        genFreeBtcAddress(walletFilePath).fold({
            return registerBtcAddressNoPreGen(irohaAccountName, keypair)
        }, { ex -> throw ex })
    }

    /**
     * Registers BTC client with no generation
     * @param irohaAccountName - client account in Iroha
     * @param keypair - key pair of new client in Iroha
     * @param whitelist - list available addresses to send money to
     * @return btc address related to client
     */
    fun registerBtcAddressNoPreGen(
        irohaAccountName: String,
        keypair: KeyPair = ModelUtil.generateKeypair(),
        whitelist: List<String> = emptyList()
    ): String {
        btcRegistrationStrategy.register(irohaAccountName, whitelist, keypair.public.toHexString())
            .fold({ btcAddress ->
                logger.info { "BTC address $btcAddress was registered by $irohaAccountName" }
                return btcAddress
            }, { ex -> throw ex })
    }

    /**
     * Sends btc to a given address
     */
    fun sendBtc(address: String, amount: Int, confirmations: Int = 6) {
        logger.info { "Send $amount BTC to $address" }
        rpcClient.sendToAddress(address = address, amount = BigDecimal(amount))
        generateBtcBlocks(confirmations)
    }

    /**
     * Send sat to a given address
     */
    fun sendSat(address: String, amount: Int, confirmations: Int = 6) {
        logger.info { "Send $amount SAT to $address" }
        rpcClient.sendToAddress(address = address, amount = satToBtc(amount.toLong()))
        generateBtcBlocks(confirmations)
    }

    /**
     * Creates blocks in bitcoin blockchain. May be used as transaction confirmation mechanism.
     */
    fun generateBtcBlocks(blocks: Int = 150) {
        if (blocks > 0) {
            rpcClient.generate(numberOfBlocks = blocks)
            logger.info { "New $blocks ${singularOrPluralBlocks(blocks)} generated in Bitcoin blockchain" }
        }
    }

    private fun singularOrPluralBlocks(blocks: Int): String {
        if (blocks == 1) {
            return "block was"
        }
        return "blocks were"
    }

    // Converts Bitcoins to Satoshi
    fun btcToSat(btc: Int): BigDecimal {
        return BigDecimal(btc * 100_000_000L)
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
