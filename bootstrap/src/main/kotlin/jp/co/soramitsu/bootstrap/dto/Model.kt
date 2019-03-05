package jp.co.soramitsu.bootstrap.dto

import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.bootstrap.genesis.getIrohaPublicKeyFromHex
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.params.TestNet3Params
import org.web3j.crypto.WalletFile
import java.security.KeyPair
import javax.validation.constraints.NotNull
import javax.xml.bind.DatatypeConverter

private interface DtoFactory<out T> {
    fun getDTO(): T
}

enum class BtcNetwork(val params:NetworkParameters) {
    RegTest(RegTestParams.get()),
    TestNet3(TestNet3Params.get()),
    MainNet(MainNetParams.get())
}

open class Conflictable(var errorCode: String? = null, var message: String? = null)

data class BlockchainCreds(
    val private: String? = null,
    val public: String? = null,
    val address: String? = null
)

data class IrohaAccountDto(
    val name: String = "",
    val domainId: String = "",
    val creds: List<BlockchainCreds> = listOf()
)

data class IrohaAccount(val title: String, val domain: String, val keys: HashSet<KeyPair>) :
    DtoFactory<IrohaAccountDto> {
    override fun getDTO(): IrohaAccountDto {
        val credsList = keys.map {
            BlockchainCreds(
                DatatypeConverter.printHexBinary(it.private.encoded),
                DatatypeConverter.printHexBinary(it.public.encoded)
            )
        }
        return IrohaAccountDto(this.title, domain, credsList)
    }
}

data class Peer(@NotNull val peerKey: String = "", @NotNull val hostPort: String = "localhost:10001")
data class AccountPublicInfo(@NotNull val pubKeys: List<String> = emptyList(), @NotNull val domainId: String? = null, @NotNull val accountName: String? = null)
data class Project(val project: String = "D3", val environment: String = "test")

data class GenesisRequest(
    val accounts: List<AccountPublicInfo> = emptyList(),
    val peers: List<Peer> = emptyList(),
    val blockVersion: String = "1",
    val meta: Project = Project()
)

data class GenesisResponse(val blockData: String? = null) :
    Conflictable()

data class EthWallet(val file: WalletFile? = null) : Conflictable()
data class BtcWallet(val file: String? = null, val network:BtcNetwork? = null) : Conflictable()

/**
 * Accounts which can't create transactions and no need to generate credentials for this accounts
 */
class PassiveAccountPrototype(
    name: String,
    domainId: String,
    roles: List<String> = emptyList(),
    details: HashMap<String, String> = HashMap(),
    quorum: Int = 1
) : AccountPrototype(
    name,
    domainId,
    roles,
    details,
    passive = true,
    quorum = quorum
) {

    fun createAccount(builder: TransactionBuilder) {
        createAccount(builder)
    }

    override fun createAccount(
        builder: TransactionBuilder,
        publicKey: String
    ) {
        createAccount(builder)
    }
}


open class AccountPrototype(
    val title: String,
    val domainId: String,
    private val roles: List<String> = listOf(),
    private val details: Map<String, String> = mapOf(),
    val passive: Boolean = false,
    val quorum: Int = 1
) {
    val id = "$title@$domainId"

    open fun createAccount(
        builder: TransactionBuilder,
        publicKey: String = "0000000000000000000000000000000000000000000000000000000000000000"
    ) {
        builder.createAccount(title, domainId, getIrohaPublicKeyFromHex(publicKey))
        roles.forEach { builder.appendRole(id, it) }
        details.forEach { k, v -> builder.setAccountDetail(id, k, v) }
    }
}
