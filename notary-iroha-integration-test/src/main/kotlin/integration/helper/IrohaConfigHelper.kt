package integration.helper

import config.IrohaConfig
import config.loadConfigs
import integration.TestConfig

/**
 *Class that handles all the configuration objects.
 */
open class IrohaConfigHelper {

    /** Configurations for tests */
    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties")

    /** Test configuration for Iroha */
    fun createIrohaConfig(
    ): IrohaConfig {
        return object : IrohaConfig {
            override val hostname = testConfig.iroha.hostname
            override val port = testConfig.iroha.port
        }
    }

    companion object {
        const val timeoutMinutes = 5L
    }
}
