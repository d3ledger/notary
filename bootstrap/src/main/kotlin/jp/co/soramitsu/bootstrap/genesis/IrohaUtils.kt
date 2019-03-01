package jp.co.soramitsu.bootstrap.genesis

import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.TransactionBuilder
import jp.co.soramitsu.bootstrap.dto.Peer
import jp.co.soramitsu.bootstrap.error.IrohaPublicKeyError
import java.lang.NullPointerException
import java.security.PublicKey
import javax.xml.bind.DatatypeConverter

fun getIrohaPublicKeyFromHexString(hex: String?): PublicKey {
    try {
        if(hex == null) {
            throw NullPointerException("Public key string should be not null")
        }
        return Ed25519Sha3.publicKeyFromBytes(DatatypeConverter.parseBase64Binary(hex))
    } catch(e:Exception) {
        throw jp.co.soramitsu.bootstrap.error.IrohaPublicKeyError("${e.javaClass}:${e.message}")
    }
}


fun createDomain(
    builder: TransactionBuilder,
    domainId: String,
    defaultRole: String
) {
    builder.createDomain(domainId, defaultRole)
}

fun createAsset(
    builder: TransactionBuilder,
    name: String,
    domain: String,
    precision: Int
) {
    builder.createAsset(name, domain, precision)
}

fun createAccount(
    builder: TransactionBuilder,
    name: String,
    domainId: String,
    publicKey: String
) {
    builder.createAccount(name, domainId, getIrohaPublicKeyFromHexString(publicKey))

}

fun createPeers(
    peers: List<jp.co.soramitsu.bootstrap.dto.Peer>,
    builder: TransactionBuilder
) {
    peers.forEach {
        builder
            .addPeer(it.hostPort, getIrohaPublicKeyFromHexString(it.peerKey))
    }
}

