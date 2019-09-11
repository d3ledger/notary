/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.config

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables

class ConfigsPriorityTest {

    @Rule
    @JvmField
    //For environment variables dependent code testing
    val environmentVariables = EnvironmentVariables()

    /**
     * @given environment variables containing account id property
     * @when test properties file is passed to loadConfigs
     * @then MixedConfig is constructed based on both environment variables and file
     */
    @Test
    fun testTestConfigEnvPartially() {
        val envAccountId = "envAccountId"
        val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties").get()
        environmentVariables.set("TEST_TESTCREDENTIALCONFIG_ACCOUNTID", envAccountId)
        val mixedConfig = loadConfigs("test", TestConfig::class.java, "/test.properties").get()
        assertEquals(envAccountId, mixedConfig.testCredentialConfig.accountId)
        assertEquals(
            testConfig.testCredentialConfig.pubkey,
            mixedConfig.testCredentialConfig.pubkey
        )
        assertEquals(
            testConfig.testCredentialConfig.privkey,
            mixedConfig.testCredentialConfig.privkey
        )
    }

    /**
     * @given environment variables containing test properties
     * @when wrong test properties file is passed to loadConfigs
     * @then EnvConfig is constructed based on environment variables values
     */
    @Test
    fun testTestConfigEnv() {
        val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties").get()
        environmentVariables.set("ENV_IROHA_HOSTNAME", testConfig.iroha.hostname)
        environmentVariables.set("ENV_IROHA_PORT", testConfig.iroha.port.toString())

        environmentVariables.set(
            "ENV_TESTCREDENTIALCONFIG_ACCOUNTID",
            testConfig.testCredentialConfig.accountId
        )
        environmentVariables.set(
            "ENV_TESTCREDENTIALCONFIG_PUBKEY",
            testConfig.testCredentialConfig.pubkey
        )
        environmentVariables.set(
            "ENV_TESTCREDENTIALCONFIG_PRIVKEY",
            testConfig.testCredentialConfig.privkey
        )
        // Pass an nonexistent file to be sure all values are loaded from the environment
        val envConfig = loadConfigs("env", TestConfig::class.java, "/NOSUCHFILE.properties").get()
        assertEquals(testConfig.iroha.hostname, envConfig.iroha.hostname)
        assertEquals(testConfig.iroha.port, envConfig.iroha.port)
        assertEquals(
            testConfig.testCredentialConfig.accountId,
            envConfig.testCredentialConfig.accountId
        )
        assertEquals(
            testConfig.testCredentialConfig.pubkey,
            envConfig.testCredentialConfig.pubkey
        )
        assertEquals(
            testConfig.testCredentialConfig.privkey,
            envConfig.testCredentialConfig.privkey
        )
    }
}
