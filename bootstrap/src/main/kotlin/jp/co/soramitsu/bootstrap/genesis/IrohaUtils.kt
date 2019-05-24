/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.bootstrap.genesis

import jp.co.soramitsu.bootstrap.dto.Peer
import jp.co.soramitsu.bootstrap.exceptions.IrohaPublicKeyException
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.TransactionBuilder
import mu.KLogging
import java.security.PublicKey
import javax.xml.bind.DatatypeConverter

private val log = KLogging().logger

fun getIrohaPublicKeyFromHex(hex: String?): PublicKey {
    try {
        if (hex == null) {
            throw NullPointerException("Public key string should be not null")
        }
        val binaryKey = DatatypeConverter.parseHexBinary(hex)
        return Ed25519Sha3.publicKeyFromBytes(binaryKey)
    } catch (e: Exception) {
        log.error("Error parsing publicKey", e)
        throw IrohaPublicKeyException("${e.javaClass}:${e.message}")
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
    builder.createAccount(name, domainId, getIrohaPublicKeyFromHex(publicKey))

}

fun createPeers(
    peers: List<Peer>,
    builder: TransactionBuilder
) {
    peers.forEach {
        builder
            .addPeer(it.hostPort, getIrohaPublicKeyFromHex(it.peerKey))
    }
}

