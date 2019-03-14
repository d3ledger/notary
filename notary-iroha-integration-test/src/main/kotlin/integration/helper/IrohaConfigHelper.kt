package integration.helper

import com.d3.commons.config.IrohaConfig
import com.d3.commons.config.loadConfigs
import com.d3.commons.registration.NotaryRegistrationConfig
import integration.TestConfig
import java.util.concurrent.atomic.AtomicInteger

/**
 *Class that handles all the configuration objects.
 */
open class IrohaConfigHelper {

    /** Configurations for tests */
    val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties").get()

    /** Configuration for registration */
    private val registrationConfig =
        loadConfigs("registration", NotaryRegistrationConfig::class.java, "/local.properties").get()

    /** Test configuration for Iroha */
    fun createIrohaConfig(
    ): IrohaConfig {
        return object : IrohaConfig {
            override val hostname = testConfig.iroha.hostname
            override val port = testConfig.iroha.port
        }
    }

    /** Test configuration of Registration with runtime dependencies */
    fun createRegistrationConfig(accountHelper: IrohaAccountHelper): NotaryRegistrationConfig {
        return object : NotaryRegistrationConfig {
            override val port = portCounter.incrementAndGet()
            override val iroha = createIrohaConfig()
            override val registrationCredential =
                accountHelper.createCredentialConfig(accountHelper.registrationAccount)
            override val clientStorageAccount = registrationConfig.clientStorageAccount
            override val brvsAccount = registrationConfig.brvsAccount
            override val primaryPubkeyPath = registrationConfig.primaryPubkeyPath
            override val primaryPrivkeyPath = registrationConfig.primaryPrivkeyPath
        }
    }

    companion object {
        const val timeoutMinutes = 5L
        /** Port counter, so new port is generated for each run */
        val portCounter = AtomicInteger(19_999)
    }
}
