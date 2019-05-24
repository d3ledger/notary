package com.d3.commons.config

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables

class ConfigsTest {

    @Rule
    @JvmField
    //For environment variables dependent code testing
    val environmentVariables = EnvironmentVariables()

    /**
     * @given the 'test_local.properties' file located at 'resources' directory
     * @when loadRawLocalConfigs() is called to retrieve configs from the test file
     * @then configs are successfully retrieved
     */
    @Test
    fun testLoadRawConfigsFromResources() {
        val testConfig = loadRawLocalConfigs("test", TestConfig::class.java, "test_local.properties")
        assertEquals("local@notary", testConfig.testCredentialConfig.accountId)
    }

    /**
     * @given the 'test_local.properties' file located at 'resources' directory and 'PROFILE' env variable being set to 'local'
     * @when loadLocalConfigs() is called to retrieve configs from the test file
     * @then local configs are successfully retrieved
     */
    @Test
    fun testLoadConfigsFromResourcesLocal() {
        environmentVariables.set("PROFILE", "local")
        val testConfig = loadLocalConfigs("test", TestConfig::class.java, "test.properties").get()
        assertEquals("local@notary", testConfig.testCredentialConfig.accountId)
    }

    /**
     * @given the 'test_testnet.properties' file located at 'resources' directory and 'PROFILE' env variable being set to 'testnet'
     * @when loadLocalConfigs() is called to retrieve configs from the test file
     * @then testnet configs are successfully retrieved
     */
    @Test
    fun testLoadConfigsFromResourcesTestnet() {
        environmentVariables.set("PROFILE", "testnet")
        val testConfig = loadLocalConfigs("test", TestConfig::class.java, "test.properties").get()
        assertEquals("testnet@notary", testConfig.testCredentialConfig.accountId)
    }

}
