package registration.relay

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.ManagedChannelBuilder
import iroha.protocol.BlockOuterClass
import iroha.protocol.CommandServiceGrpc
import iroha.protocol.Endpoint
import jp.co.soramitsu.iroha.Keypair
import jp.co.soramitsu.iroha.ModelProtoTransaction
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import main.ConfigKeys
import mu.KLogging
import notary.EthTokensProvider
import notary.EthTokensProviderImpl
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import sidechain.iroha.consumer.IrohaKeyLoader
import sidechain.iroha.util.toByteArray
import java.math.BigInteger

/**
 * Class is responsible for relay addresses registration.
 * Deploys relay smart contracts in Ethereum network and records it in Iroha.
 */
class RelayRegistration(
    val ethTokensProvider: EthTokensProvider = EthTokensProviderImpl()
) {

    /** web3 service instance to communicate with Ethereum network */
    private val web3 = Web3j.build(HttpService(CONFIG[ConfigKeys.ethConnectionUrl]))

    /** credentials of ethereum user */
    private val credentials =
        WalletUtils.loadCredentials(CONFIG[ConfigKeys.ethCredentialPassword], CONFIG[ConfigKeys.ethCredentialPath])

    /** Gas price */
    private val gasPrice = BigInteger.valueOf(CONFIG[ConfigKeys.ethGasPrice])

    /** Max gas limit */
    private val gasLimit = BigInteger.valueOf(CONFIG[ConfigKeys.ethGasLimit])

    /** Iroha keypair */
    val keypair: Keypair =
        IrohaKeyLoader.loadKeypair(CONFIG[ConfigKeys.pubkeyPath], CONFIG[ConfigKeys.privkeyPath]).get()

    /** Iroha host */
    val irohaHost = CONFIG[ConfigKeys.irohaHostname]

    /** Iroha port */
    val irohaPort = CONFIG[ConfigKeys.irohaPort]

    /** Iroha transaction creator */
    val creator = CONFIG[ConfigKeys.irohaCreator]

    /** Notary master account */
    val masterAccount = CONFIG[ConfigKeys.irohaMaster]

    /**
     * Deploy user smart contract
     * @param master notary master account
     * @param tokens list of supported tokens
     * @return user smart contract address
     */
    private fun deployRelaySmartContract(master: String, tokens: List<String>): String {
        val contract =
            contract.User.deploy(
                web3,
                credentials,
                gasPrice,
                gasLimit,
                master,
                tokens
            ).send()

        logger.info { "Relay wallet created with address ${contract.contractAddress}" }
        return contract.contractAddress
    }

    /**
     * Sends transaction to Iroha.
     * @param wallet - ethereum wallet to record into Iroha
     * @return hex representation of transaction
     */
    private fun sendRelayToIroha(wallet: String): String {
        val currentTime = System.currentTimeMillis()
        val utx = ModelTransactionBuilder().creatorAccountId(creator)
            .createdTime(BigInteger.valueOf(currentTime))
            .setAccountDetail(masterAccount, wallet, "free")
            .build()
        val hash = utx.hash().hex()

        // sign transaction and get its binary representation (Blob)
        val txblob = ModelProtoTransaction(utx).signAndAddSignature(keypair).finish().blob().toByteArray()

        // create proto object
        val protoTx: BlockOuterClass.Transaction?
        try {
            protoTx = BlockOuterClass.Transaction.parseFrom(txblob)
        } catch (e: InvalidProtocolBufferException) {
            logger.error { "Exception while converting byte array to protobuf: ${e.message}" }
            throw Exception("Exception while converting byte array to protobuf: ${e.message}")
        }

        // Send transaction to iroha
        val channel = ManagedChannelBuilder.forAddress(irohaHost, irohaPort).usePlaintext(true).build()
        val stub = CommandServiceGrpc.newBlockingStub(channel)
        stub.torii(protoTx)

        // wait to ensure transaction was processed
        runBlocking { delay(5000) }

        // create status request
        logger.info { "Send Iroha transaction: $hash" }

        val bshash = utx.hash().blob().toByteArray()
        val request = Endpoint.TxStatusRequest.newBuilder().setTxHash(ByteString.copyFrom(bshash)).build()
        val response = stub.status(request)
        val status = response.txStatus.name

        logger.info { "Status of iroha transaction $hash is: $status" }

        if (status != "COMMITTED") {
            logger.error { "$hash transaction wasn't committed" }
            throw Exception("$hash transaction wasn't committed")
        }

        return hash
    }

    /**
     * Deploy [num] relay wallets
     * @param num - number of wallets to deploy
     */
    fun deploy(num: Int, master: String): Result<Unit, Exception> {
        val tokens = ethTokensProvider.getTokens()
        return tokens.map { token ->
            (1..num).forEach {
                val relayWallet = deployRelaySmartContract(master, token.keys.toList())
                sendRelayToIroha(relayWallet)
            }
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()

}
