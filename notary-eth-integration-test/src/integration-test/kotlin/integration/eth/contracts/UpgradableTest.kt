package integration.eth.contracts

import contract.TestGreeter_v0
import contract.TestGreeter_v1
import helper.encodeFunction
import integration.helper.ContractTestHelper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.Utf8String
import org.web3j.protocol.exceptions.TransactionException
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger
import kotlin.test.assertEquals

class UpgradableTest {

    val chr = ContractTestHelper()

    /**
     * Test basic upgrade behavior
     * @given proxy contract pointing to implementation v0
     * @when proxy is upgraded to implementation v1
     * @then behavior of implementation v1 is called through proxy
     */
    @Test
    fun testUpgradable() {
        // deploy implementation v0
        val expectedInitial = "initial string"
        val implementation_v0 = chr.deployHelper.deployTestGreeter_v0(expectedInitial)
        assertEquals(expectedInitial, implementation_v0.greet().send())

        // deploy proxy
        val proxy = chr.deployHelper.deployOwnedUpgradeabilityProxy()

        // set proxy to implementation v0 and initialize
        val proxyInitial = "proxy initial"
        val encoded = encodeFunction("initialize", Utf8String(proxyInitial) as Type<Any>)
        proxy.upgradeToAndCall(implementation_v0.contractAddress, encoded, BigInteger.ZERO).send()

        // load contract via proxy
        val implProxy_v0 = TestGreeter_v0.load(
            proxy.contractAddress,
            chr.deployHelper.web3,
            chr.deployHelper.credentials,
            StaticGasProvider(chr.deployHelper.gasPrice, chr.deployHelper.gasLimit)
        )
        assertEquals(proxyInitial, implProxy_v0.greet().send())


        // ============ upgrade ============
        // deploy implementation v1
        val implementation_v1 = chr.deployHelper.deployTestGreeter_v1()

        // upgrade proxy to contract v1
        proxy.upgradeTo(implementation_v1.contractAddress).send()

        // load contract via proxy
        val farevell = "Good bye!"
        val implProxy_v1 = TestGreeter_v1.load(
            proxy.contractAddress,
            chr.deployHelper.web3,
            chr.deployHelper.credentials,
            StaticGasProvider(chr.deployHelper.gasPrice, chr.deployHelper.gasLimit)
        )
        assertEquals(farevell, implProxy_v1.farewell().send())
    }

    /**
     * Forbid set the same implementation
     * @given proxy is deployed and set to an implementation
     * @when upgrade to the same implementation is called
     * @then Exception is thrown
     */
    @Test
    fun upgradeToTheSameImpl() {
        // deploy implementation v0
        val expectedInitial = "initial string"
        val implementation = chr.deployHelper.deployTestGreeter_v0(expectedInitial)
        assertEquals(expectedInitial, implementation.greet().send())

        // deploy proxy
        val proxy = chr.deployHelper.deployOwnedUpgradeabilityProxy()

        // set proxy to implementation
        proxy.upgradeTo(implementation.contractAddress).send()

        // upgrade again
        Assertions.assertThrows(TransactionException::class.java) {
            proxy.upgradeTo(implementation.contractAddress).send()
        }
    }

    /**
     * Transfer ownership
     * @given proxy is deployed
     * @when transfer ownership is called
     * @then previous owner can't call methods with proxy owner
     */
    @Test
    fun transferOwnership() {
        val proxy = chr.deployHelper.deployOwnedUpgradeabilityProxy()
        proxy.transferProxyOwnership("0x0000000000000000000000000000000000000123").send()
        val implementation = chr.deployHelper.deployTestGreeter_v0("initial string")
        Assertions.assertThrows(TransactionException::class.java) {
            proxy.upgradeTo(implementation.contractAddress).send()
        }
    }

}

