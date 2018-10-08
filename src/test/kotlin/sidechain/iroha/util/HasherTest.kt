package sidechain.iroha.util

import com.github.kittinunf.result.failure
import config.loadConfigs
import jp.co.soramitsu.iroha.Blob
import jp.co.soramitsu.iroha.ModelTransactionBuilder
import jp.co.soramitsu.iroha.iroha
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sidechain.iroha.IrohaInitialization
import kotlin.test.assertEquals

class HasherTest {

    init {
        IrohaInitialization.loadIrohaLibrary()
            .failure { fail(it) }
    }

    /** Test configurations */
    private val credentialConfig =
        loadConfigs("test.testCredentialConfig", config.IrohaCredentialConfig::class.java, "/test.properties")

    /** Test keypair */
    private val keypair = ModelUtil.loadKeypair(
        credentialConfig.pubkeyPath,
        credentialConfig.privkeyPath
    ).get()

    /**
     * @given protobuf bytes of transaction
     * @when hashing is called
     * @then hash should be returned in hexadecimal format
     */
    @Test
    fun testTransferAssetHash() {

        val tx = ModelTransactionBuilder()
            .creatorAccountId(credentialConfig.accountId)
            .createdTime(ModelUtil.getCurrentTime())
            .quorum(1)
            .transferAsset("from@test", "to@test", "coin#test", "descr", "123")
            .build()
            .signAndAddSignature(keypair)
        val bytes = tx.finish().blob().blob().toByteArray()

        val expectedHash = tx.hash().hex()

        val hash = Blob(iroha.hashTransaction(bytes.toByteVector())).hex()
        assertEquals(expectedHash, hash)
    }
}
