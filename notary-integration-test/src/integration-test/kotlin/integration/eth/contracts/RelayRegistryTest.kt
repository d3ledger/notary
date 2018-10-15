package integration.eth.contracts

import contract.RelayRegistry
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.Keys
import org.web3j.protocol.exceptions.TransactionException

class RelayRegistryTest {
    private val cth = ContractTestHelper()
    private lateinit var relayRegistry: RelayRegistry
    private val accGreen = cth.accGreen
    private val keypair = cth.keypair

    @BeforeEach
    fun setup() {
        cth.deployContracts()
        relayRegistry = cth.relayRegistry
    }

    /**
     * @given deployed relay registry contract
     * @when add new relay twice
     * @then second call throws exception
     */
    @Test
    fun addSameWhiteListTwice() {
        cth.addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen)) // first call
        Assertions.assertThrows(TransactionException::class.java) {
            cth.addWhiteListToRelayRegistry(
                Keys.getAddress(keypair),
                listOf(accGreen)
            )
        } // second call
    }

    /**
     * @given deployed relay registry contract
     * @when add new white list and check address in white list
     * @then address should be in the list
     */
    @Test
    fun addWhiteListAndCheckAddress() {
        cth.addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf(accGreen))
        Assertions.assertTrue(relayRegistry.isWhiteListed(Keys.getAddress(keypair), accGreen).send())
    }

    /**
     * @given relay registry contract
     * @when add list of 100 addresses
     * @then all addresses should be stored successful
     */
    @Test
    fun addOneHundredAddressesInWhiteListAndCheckThem() {
        val whiteListSize = 100
        var equalAddressesSize = 0
        val whiteList = mutableListOf<String>()
        for (i in 0 until whiteListSize) {
            val keypair = Keys.createEcKeyPair()
            whiteList.add(Keys.getAddress(keypair))
        }

        val addressList: List<String> = whiteList.toList()

        cth.addWhiteListToRelayRegistry(Keys.getAddress(keypair), addressList)

        for (i in 0 until addressList.size) {
            if (relayRegistry.isWhiteListed(
                    Keys.getAddress(keypair),
                    addressList[i]
                ).send()
            ) {
                equalAddressesSize++
            }
        }

        assertEquals(equalAddressesSize, relayRegistry.getWhiteListByRelay(Keys.getAddress(keypair)).send().size)
    }

    // TODO: make test for empty whitelist in smart contract
    /**
     * @given relay registry contract
     * @when try to store empty list
     * @then transaction should be successful
     */
    @Test
    fun setEmptyList() {
        val call = cth.addWhiteListToRelayRegistry(Keys.getAddress(keypair), listOf())
        assertEquals(true, call.isStatusOK)
    }
}
