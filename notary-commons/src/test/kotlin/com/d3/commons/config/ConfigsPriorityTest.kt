package com.d3.commons.config

import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.EnvironmentVariables


class ConfigsPriorityTest {

    @Rule
    @JvmField
    //For environment variables dependent code testing
    val environmentVariables = EnvironmentVariables()

    /**
     * @given command line array full of passwords
     * @when command line is passed to loadEthPasswords()
     * @then EthPasswordsConfig is constructed based on command line
     */
    @Test
    @Ignore
    fun testLoadEthPasswordsArgs() {
        val args = arrayOf("argCredentialsPassword", "argNodeLogin", "argNodePassword")
        val ethPasswords = loadEthPasswords("test", "/eth/ethereum_password.properties", args).get()
        assertEquals(args[0], ethPasswords.credentialsPassword)
        assertEquals(args[1], ethPasswords.nodeLogin)
        assertEquals(args[2], ethPasswords.nodePassword)
    }

    /**
     * @given command line array and environment variables full of passwords
     * @when command line is passed to loadEthPasswords()
     * @then EthPasswordsConfig is constructed based on command line
     */
    @Test
    @Ignore
    fun testLoadEthPasswordsArgsWithEnvVariables() {
        setEthEnvVariables()
        val args = arrayOf("argCredentialsPassword", "argNodeLogin", "argNodePassword")
        val ethPasswords = loadEthPasswords("test", "/eth/ethereum_password.properties", args).get()
        assertEquals(args[0], ethPasswords.credentialsPassword)
        assertEquals(args[1], ethPasswords.nodeLogin)
        assertEquals(args[2], ethPasswords.nodePassword)
    }

    /**
     * @given environment variables full of passwords
     * @when properties file is passed to loadEthPasswords()
     * @then EthPasswordsConfig is constructed based on environment variables
     */
    @Test
    fun testLoadEthPasswordsEnv() {
        setEthEnvVariables()
        val envCredentialsPassword = System.getenv(ETH_CREDENTIALS_PASSWORD_ENV)
        val envNodeLogin = System.getenv(ETH_NODE_LOGIN_ENV)
        val envNodePassword = System.getenv(ETH_NODE_PASSWORD_ENV)
        val ethPasswords = loadEthPasswords("test", "/eth/ethereum_password.properties").get()
        assertEquals(envCredentialsPassword, ethPasswords.credentialsPassword)
        assertEquals(envNodeLogin, ethPasswords.nodeLogin)
        assertEquals(envNodePassword, ethPasswords.nodePassword)
    }

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
        assertEquals(testConfig.testCredentialConfig.pubkeyPath, mixedConfig.testCredentialConfig.pubkeyPath)
        assertEquals(testConfig.testCredentialConfig.privkeyPath, mixedConfig.testCredentialConfig.privkeyPath)
    }

    /**
     * @given environment variables containing test properties
     * @when wrong test properties file is passed to loadConfigs
     * @then EnvConfig is constructed based on environment variables values
     */
    @Test
    fun testTestConfigEnv() {
        val testConfig = loadConfigs("test", TestConfig::class.java, "/test.properties").get()
        environmentVariables.set("ENV_ETHTESTACCOUNT", testConfig.ethTestAccount)
        environmentVariables.set("ENV_IROHA", "")
        environmentVariables.set("ENV_IROHA_HOSTNAME", testConfig.iroha.hostname)
        environmentVariables.set("ENV_IROHA_PORT", testConfig.iroha.port.toString())
        environmentVariables.set("ENV_ETHEREUM", "")
        environmentVariables.set("ENV_ETHEREUM_URL", testConfig.ethereum.url)
        environmentVariables.set("ENV_ETHEREUM_CONFIRMATIONPERIOD", testConfig.ethereum.confirmationPeriod.toString())
        environmentVariables.set("ENV_ETHEREUM_CREDENTIALSPATH", testConfig.ethereum.credentialsPath)
        environmentVariables.set("ENV_ETHEREUM_GASPRICE", testConfig.ethereum.gasPrice.toString())
        environmentVariables.set("ENV_ETHEREUM_GASLIMIT", testConfig.ethereum.gasLimit.toString())
        environmentVariables.set("ENV_TESTCREDENTIALCONFIG", "")
        environmentVariables.set("ENV_TESTCREDENTIALCONFIG_ACCOUNTID", testConfig.testCredentialConfig.accountId)
        environmentVariables.set("ENV_TESTCREDENTIALCONFIG_PUBKEYPATH", testConfig.testCredentialConfig.pubkeyPath)
        environmentVariables.set("ENV_TESTCREDENTIALCONFIG_PRIVKEYPATH", testConfig.testCredentialConfig.privkeyPath)
        // Pass an nonexistent file to be sure all values are loaded from the environment
        val envConfig = loadConfigs("env", TestConfig::class.java, "/NOSUCHFILE.properties").get()
        assertEquals(testConfig.ethTestAccount, envConfig.ethTestAccount)
        assertEquals(testConfig.iroha.hostname, envConfig.iroha.hostname)
        assertEquals(testConfig.iroha.port, envConfig.iroha.port)
        assertEquals(testConfig.ethereum.url, envConfig.ethereum.url)
        assertEquals(testConfig.ethereum.confirmationPeriod, envConfig.ethereum.confirmationPeriod)
        assertEquals(testConfig.ethereum.credentialsPath, envConfig.ethereum.credentialsPath)
        assertEquals(testConfig.ethereum.gasPrice, envConfig.ethereum.gasPrice)
        assertEquals(testConfig.ethereum.gasLimit, envConfig.ethereum.gasLimit)
        assertEquals(testConfig.testCredentialConfig.accountId, envConfig.testCredentialConfig.accountId)
        assertEquals(testConfig.testCredentialConfig.pubkeyPath, envConfig.testCredentialConfig.pubkeyPath)
        assertEquals(testConfig.testCredentialConfig.privkeyPath, envConfig.testCredentialConfig.privkeyPath)
    }

    private fun setEthEnvVariables() {
        environmentVariables.set(ETH_CREDENTIALS_PASSWORD_ENV, "env_credentialsPassword")
        environmentVariables.set(ETH_NODE_LOGIN_ENV, "env_nodeLogin")
        environmentVariables.set(ETH_NODE_PASSWORD_ENV, "env_nodePassword")
    }
}
