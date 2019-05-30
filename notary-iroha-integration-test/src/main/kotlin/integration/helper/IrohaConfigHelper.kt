/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

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
                accountHelper.createCredentialRawConfig(accountHelper.registrationAccount)
            override val clientStorageAccount = testConfig.clientStorageAccount
            override val brvsAccount = testConfig.brvsAccount
            override val primaryPubkey = registrationCredential.pubkey
            override val primaryPrivkey = registrationCredential.privkey
        }
    }

    companion object {
        const val timeoutMinutes = 5L
        /** Port counter, so new port is generated for each run */
        val portCounter = AtomicInteger(19_999)
    }
}
