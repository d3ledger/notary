package config

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

    private fun setEthEnvVariables() {
        environmentVariables.set(ETH_CREDENTIALS_PASSWORD_ENV, "env_credentialsPassword")
        environmentVariables.set(ETH_NODE_LOGIN_ENV, "env_nodeLogin")
        environmentVariables.set(ETH_NODE_PASSWORD_ENV, "env_nodePassword")
    }
}
