package registration.eth.relay

import config.loadConfigs
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RelayEthRegistrationConfigTest {

    /**
     * @given a config with correct default values
     * @when config is loaded
     * @then relayRegistrationConfig object is returned and all configuration parameters are set
     */
    @Test
    fun allSetTest() {
        val relayRegistrationConfig =
            loadConfigs("relay-registration", RelayRegistrationConfig::class.java, "/eth/relay_registration.properties")

        assertEquals(10, relayRegistrationConfig.number)
        assertEquals("notary_red@notary", relayRegistrationConfig.notaryIrohaAccount)
        assertEquals("0x57dd50c6b4ec7a33ee3bb5056bf38856e181684c", relayRegistrationConfig.ethMasterWallet)

        assertEquals("d3-iroha", relayRegistrationConfig.iroha.hostname)
        assertEquals(50051, relayRegistrationConfig.iroha.port)


        assertEquals("http://d3-eth-node0:8545", relayRegistrationConfig.ethereum.url)
        assertEquals("deploy/ethereum/keys/ganache.key", relayRegistrationConfig.ethereum.credentialsPath)
        assertEquals(1, relayRegistrationConfig.ethereum.gasPrice)
        assertEquals(1999999, relayRegistrationConfig.ethereum.gasLimit)
    }
}
