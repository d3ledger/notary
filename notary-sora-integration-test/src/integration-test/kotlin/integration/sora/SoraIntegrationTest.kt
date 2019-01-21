package integration.sora

import integration.helper.IrohaIntegrationHelperUtil
import jp.co.soramitsu.crypto.ed25519.Ed25519Sha3
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import sidechain.iroha.util.ModelUtil
import util.getRandomString
import kotlin.test.assertEquals

/**
 * This test case tests Sora integration.
 * All accounts are created and have permissions.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SoraIntegrationTest {

    val integrationHelper = IrohaIntegrationHelperUtil()

    val domain = "sora"
    val xorAsset = "xor#$domain"

    init {
        System.loadLibrary("irohajava")
    }

    /**
     * Sora user can query her/his balance.
     * @given a client with 1334 xor on balance
     * @when the client queries the balance
     * @then the actual balance of 1334 xor is returned
     */
    @Test
    fun getXorBalanceTest() {
        val clientName = String.getRandomString(9)
        val clientId = "$clientName@$domain"

        val keypairAlice = Ed25519Sha3().generateKeypair()

        integrationHelper.createAccount(clientName, domain, keypairAlice.public)

        assertEquals(
            "0",
            integrationHelper.getAccountAssets(clientId).getOrDefault(xorAsset, "0")
        )

        integrationHelper.addIrohaAssetTo(clientId, xorAsset, "1334")
        assertEquals(
            "1334",
            integrationHelper.getAccountAssets(clientId).getOrDefault(xorAsset, "0")
        )
    }

    /**
     * Sora user transfer XOR to another client.
     * @given a client alice with 1334 xor and a client bob with 0 xor
     * @when alice transfer 1330 xor to bob
     * @then alice balance is 4 xor and bob balance is 1330 xor now
     */
    @Test
    fun transferSoraTest() {
        val aliceClientName = String.getRandomString(9)
        val aliceClientId = "$aliceClientName@$domain"
        val keypairAlice = Ed25519Sha3().generateKeypair()
        integrationHelper.createAccount(aliceClientName, domain, keypairAlice.public)

        val bobClientName = String.getRandomString(9)
        val bobClientId = "$bobClientName@$domain"
        val keypairBob = Ed25519Sha3().generateKeypair()
        integrationHelper.createAccount(bobClientName, domain, keypairBob.public)

        integrationHelper.addIrohaAssetTo(aliceClientId, xorAsset, "1334")

        integrationHelper.transferAssetIrohaFromClient(
            aliceClientId,
            keypairAlice,
            aliceClientId,
            bobClientId,
            xorAsset,
            "descr",
            "1330"
        )

        assertEquals(
            "4",
            integrationHelper.getIrohaAccountBalance(aliceClientId, xorAsset)
        )
        assertEquals(
            "1330",
            integrationHelper.getIrohaAccountBalance(bobClientId, xorAsset)
        )
    }

    /**
     * Sora distribution.
     *
     * @given sora account with 35 xor and alice with 0 xor and bob with 0 xor
     * @when distribute 17 to alice and 18 to bob
     * @then alice has 17 xor and bob has 18 xor
     */
    @Test
    fun distributeSoraTest() {
        val aliceClientName = String.getRandomString(9)
        val aliceClientId = "$aliceClientName@$domain"
        val keypairAlice = Ed25519Sha3().generateKeypair()
        integrationHelper.createAccount(aliceClientName, domain, keypairAlice.public)

        val bobClientName = String.getRandomString(9)
        val bobClientId = "$bobClientName@$domain"
        val keypairBob = Ed25519Sha3().generateKeypair()
        integrationHelper.createAccount(bobClientName, domain, keypairBob.public)

        val soraClientId = "sora@sora"
        val soraKeyPair =
            ModelUtil.loadKeypair("deploy/iroha/keys/sora@sora.pub", "deploy/iroha/keys/sora@sora.priv").get()
        integrationHelper.addIrohaAssetTo(soraClientId, xorAsset, "35")

        integrationHelper.transferAssetIrohaFromClient(
            soraClientId,
            soraKeyPair,
            soraClientId,
            aliceClientId,
            xorAsset,
            "descr",
            "17"
        )

        integrationHelper.transferAssetIrohaFromClient(
            soraClientId,
            soraKeyPair,
            soraClientId,
            bobClientId,
            xorAsset,
            "descr",
            "18"
        )

        assertEquals(
            "17",
            integrationHelper.getIrohaAccountBalance(aliceClientId, xorAsset)
        )
        assertEquals(
            "18",
            integrationHelper.getIrohaAccountBalance(bobClientId, xorAsset)
        )
    }

}
