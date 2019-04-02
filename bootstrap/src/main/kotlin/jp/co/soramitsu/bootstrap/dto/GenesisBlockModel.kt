package jp.co.soramitsu.bootstrap.dto

import jp.co.soramitsu.bootstrap.genesis.getIrohaPublicKeyFromHex
import jp.co.soramitsu.iroha.java.TransactionBuilder


data class GenesisRequest(
    val accounts: List<AccountPublicInfo> = emptyList(),
    val peers: List<Peer> = emptyList(),
    val blockVersion: String = "1",
    val meta: ProjectEnv = ProjectEnv()
)

data class GenesisResponse(val blockData: String? = null) :
    Conflictable() {
    constructor(errorCode: String? = null, message: String? = null) : this() {
        this.errorCode = errorCode
        this.message = message
    }
}

data class NeededAccountsResponse(val accounts: List<AccountPrototype> = emptyList()) : Conflictable() {
    constructor(errorCode: String? = null, message: String? = null) : this() {
        this.errorCode = errorCode
        this.message = message
    }
}
/**
 * Accounts which should have quorum 2/3 of peers
 */
class PeersCountDependentAccountPrototype(
    name: String? = null,
    domainId: String? = null,
    roles: List<String> = emptyList(),
    details: HashMap<String, String> = HashMap()
) : AccountPrototype(
    name,
    domainId,
    roles,
    details,
    passive = false,
    quorum = 1,
    peersDependentQuorum = true
)

/**
 * Accounts which can't create transactions and no need to generate credentials for this accounts
 */
class PassiveAccountPrototype(
    name: String? = null,
    domainId: String? = null,
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
    val name: String? = null,
    val domainId: String? = null,
    val roles: List<String> = listOf(),
    val details: Map<String, String> = mapOf(),
    val passive: Boolean = false,
    var quorum: Int = 1,
    val peersDependentQuorum: Boolean = false
) {
    val id = "$name@$domainId"

    open fun createAccount(
        builder: TransactionBuilder,
        publicKey: String = "0000000000000000000000000000000000000000000000000000000000000000"
    ) {
        builder.createAccount(name, domainId, getIrohaPublicKeyFromHex(publicKey))
        roles.forEach { builder.appendRole(id, it) }
        details.forEach { k, v -> builder.setAccountDetail(id, k, v) }
    }
}
