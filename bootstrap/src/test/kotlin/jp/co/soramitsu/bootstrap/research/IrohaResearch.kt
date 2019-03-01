package jp.co.soramitsu.bootstrap.research

import com.google.protobuf.util.JsonFormat
import iroha.protocol.BlockOuterClass
import iroha.protocol.Primitive
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import jp.co.soramitsu.iroha.java.Transaction
import jp.co.soramitsu.iroha.testcontainers.detail.GenesisBlockBuilder
import mu.KLogging
import org.junit.Test
import java.math.BigDecimal

class IrohaResearch {
    private val log = KLogging().logger

    private val bankDomain = "bank"
    private val userRole = "user"
    private val usdName = "usd"

    private val crypto = Ed25519Sha3()

    private val peerKeypair = crypto.generateKeypair()

    private val useraKeypair = crypto.generateKeypair()
    private val userbKeypair = crypto.generateKeypair()

    private fun user(name: String): String {
        return String.format("%s@%s", name, bankDomain)
    }

    private val usd = String.format("%s#%s", usdName, bankDomain)

    @Test
    fun genesisBlockTest() {
        val block: BlockOuterClass.Block = GenesisBlockBuilder()
            // first transaction
            .addTransaction(
                // transactions in genesis block can have no creator
                Transaction.builder(null)
                    // by default peer is listening on port 10001
                    .addPeer("0.0.0.0:10001", peerKeypair.getPublic())
                    // create default "user" role
                    .createRole(
                        userRole,
                        listOf(
                            Primitive.RolePermission.can_transfer,
                            Primitive.RolePermission.can_get_my_acc_ast,
                            Primitive.RolePermission.can_get_my_txs,
                            Primitive.RolePermission.can_receive
                        )
                    )
                    .createDomain(bankDomain, userRole)
                    // create user A
                    .createAccount("user_a", bankDomain, useraKeypair.getPublic())
                    // create user B
                    .createAccount("user_b", bankDomain, userbKeypair.getPublic())
                    // create usd#bank with precision 2
                    .createAsset(usdName, bankDomain, 2)
                    // transactions in genesis block can be unsigned
                    .build() // returns ipj model Transaction
                    .build() // returns unsigned protobuf Transaction
            )
            // we want to increase user_a balance by 100 usd
            .addTransaction(
                Transaction.builder(user("user_a"))
                    .addAssetQuantity(usd, BigDecimal("100"))
                    .build()
                    .build()
            ).build()
        val json = JsonFormat.printer().print(block)

        log.info("Block json: $json")
    }
}