package config

import com.github.kittinunf.result.Result
import com.jdiazcano.cfg4k.loaders.EnvironmentConfigLoader
import com.jdiazcano.cfg4k.loaders.PropertyConfigLoader
import com.jdiazcano.cfg4k.providers.DefaultConfigProvider
import com.jdiazcano.cfg4k.providers.OverrideConfigProvider
import com.jdiazcano.cfg4k.providers.ProxyConfigProvider
import com.jdiazcano.cfg4k.sources.ConfigSource
import mu.KLogging
import java.io.File
import java.io.InputStream

//Environment variable that holds Ethereum credentials password
const val ETH_CREDENTIALS_PASSWORD_ENV = "ETH_CREDENTIALS_PASSWORD"
//Environment variable that holds Ethereum node login
const val ETH_NODE_LOGIN_ENV = "ETH_NODE_LOGIN"
//Environment variable that holds Ethereum node password
const val ETH_NODE_PASSWORD_ENV = "ETH_NODE_PASSWORD"
//Environment variable that holds current application profile
const val PROFILE_ENV = "PROFILE"
//Environment variable that holds address of the master wallet
const val ETH_MASTER_WALLET_ENV = "ETH_MASTER_WALLET"
//Environment variable that holds address of the Relay implementation contract
const val ETH_RELAY_IMPLEMENTATION_ADDRESS_ENV = "ETH_RELAY_IMPLEMENTATION_ADDRESS"
//Environment variable that holds address of the relay registry
const val ETH_RELAY_REGISTRY_ENV = "ETH_RELAY_REGISTRY"


private val logger = KLogging().logger

/**
 * RMQ configurations
 */

interface RMQConfig {
    val host: String
    val irohaExchange: String

    val irohaCredential : IrohaCredentialConfig
    val iroha: IrohaConfig
}

/**
 * Iroha configurations
 */
interface IrohaConfig {
    val hostname: String
    val port: Int
}

/**
 * Configuration for Iroha credential
 */
interface IrohaCredentialConfig {
    val accountId: String
    val pubkeyPath: String
    val privkeyPath: String
}

/**
 * Ethereum configurations
 */
interface EthereumConfig {
    val url: String
    val credentialsPath: String
    val gasPrice: Long
    val gasLimit: Long
    val confirmationPeriod: Long
}

/**
 * Bitcoin configurations
 */
interface BitcoinConfig {
    //Path of block storage folder
    val blockStoragePath: String
    //Depth of transactions in BTC blockchain
    val confidenceLevel: Int
    //BTC node hosts
    val hosts: String

    companion object {
        fun extractHosts(bitcoinConfig: BitcoinConfig) = bitcoinConfig.hosts.replace(" ", "").split(",")
    }
}

/**
 * Ethereum passwords
 */
interface EthereumPasswords {
    val credentialsPassword: String
    val nodeLogin: String?
    val nodePassword: String?
}

/**
 * Returns current profile based on environment variable
 */
fun getProfile(): String {
    var profile = System.getenv(PROFILE_ENV)
    if (profile == null) {
        logger.warn { "No profile set. Using default local profile" }
        profile = "local"
    }
    return profile
}

/**
 * Load configs from Java properties
 */
fun <T : Any> loadConfigs(
    prefix: String,
    type: Class<T>,
    filename: String,
    vararg validators: ConfigValidationRule<T>
): Result<T, Exception> {
    return Result.of { loadValidatedConfigs(prefix, type, filename, *validators) }
}

/**
 * Returns default D3 config folder
 */
fun getConfigFolder() = System.getProperty("user.dir") + "/configs"

private fun <T : Any> loadValidatedConfigs(
    prefix: String,
    type: Class<T>,
    filename: String,
    vararg validators: ConfigValidationRule<T>
): T {
    val profile = getProfile()
    val (file, extension) = filename.split(".")
    val path = "${getConfigFolder()}${file}_$profile.$extension"
    logger.info { "Loading config from $path, prefix $prefix" }
    val config = loadRawConfigs(prefix, type, path)
    validators.forEach { rule -> rule.validate(config) }
    return config
}

class Stream(private val stream: InputStream) : ConfigSource {
    override fun read(): InputStream {
        return stream
    }
}

fun <T : Any> loadRawConfigs(prefix: String, type: Class<T>, filename: String): T {
    val stream = Stream(File(filename).inputStream())
    val configLoader = PropertyConfigLoader(stream)
    val envLoader = EnvironmentConfigLoader()
    val provider = OverrideConfigProvider(
        DefaultConfigProvider(envLoader),
        ProxyConfigProvider(configLoader)
    )
    return provider.bind(prefix, type)
}

/**
 * Loads ETH passwords. Lookup priority: environment variables>property file
 * TODO: implement command line argument parsing
 */
fun loadEthPasswords(
    prefix: String,
    filename: String,
    args: Array<String> = emptyArray()
): Result<EthereumPasswords, Exception> {
    var config = loadConfigs(prefix, EthereumPasswords::class.java, filename).get()

    config = object : EthereumPasswords {
        override val credentialsPassword = System.getenv(ETH_CREDENTIALS_PASSWORD_ENV) ?: config.credentialsPassword
        override val nodeLogin = System.getenv(ETH_NODE_LOGIN_ENV) ?: config.nodeLogin
        override val nodePassword = System.getenv(ETH_NODE_PASSWORD_ENV) ?: config.nodePassword
    }

    return Result.of(config)
}

/**
 * Creates ETH passwords from command line arguments
 * [0]->credentialsPassword
 * [1]->nodeLogin
 * [2]->nodePassword
 * @param args command line arguments
 * @return config full of passwords or null if no arguments were provided
 */
private fun createEthPasswordsFromArgs(args: Array<String>): EthereumPasswords? {
    if (args.size != 3)
        return null
    return object : EthereumPasswords {
        override val credentialsPassword = args[0]
        override val nodeLogin = args[1]
        override val nodePassword = args[2]
    }
}

/**
 * Creates ETH passwords from environment variables
 * @return config full of passwords or null if no env variables were set
 */
private fun createEthPasswordsFromEnv(): EthereumPasswords? {
    val envCredentialsPassword = System.getenv(ETH_CREDENTIALS_PASSWORD_ENV)
    val envNodeLogin = System.getenv(ETH_NODE_LOGIN_ENV)
    val envNodePassword = System.getenv(ETH_NODE_PASSWORD_ENV)
    if (envCredentialsPassword == null || envNodeLogin == null || envNodePassword == null) {
        return null
    }
    return object : EthereumPasswords {
        override val credentialsPassword = envCredentialsPassword
        override val nodeLogin = envNodeLogin
        override val nodePassword = envNodePassword
    }
}
