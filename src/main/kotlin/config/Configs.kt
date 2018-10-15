package config

import com.jdiazcano.cfg4k.loaders.PropertyConfigLoader
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
//Environment variable that holds address of the relay registry
const val ETH_RELAY_REGISTRY_ENV = "ETH_RELAY_REGISTRY"


private val logger = KLogging().logger

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
    //Path of wallet file
    val walletPath: String
    //Path of block storage folder
    val blockStoragePath: String
    //Depth of transactions in BTC blockchain
    val confidenceLevel: Int
    //BTC node host name
    val host: String
}

/**
 * Ethereum passwords
 */
interface EthereumPasswords {
    val credentialsPassword: String
    val nodeLogin: String
    val nodePassword: String
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
fun <T : Any> loadConfigs(prefix: String, type: Class<T>, filename: String): T {
    val profile = getProfile()
    val (file, extension) = filename.split(".")
    val pwd = System.getProperty("user.dir")
    val path = "$pwd/configs${file}_$profile.$extension"
    logger.info { "Loading config from $path, prefix $prefix" }
    return loadRawConfigs(prefix, type, path)
}

class Stream(private val stream: InputStream) : ConfigSource {
    override fun read(): InputStream {
        return stream
    }
}

fun <T : Any> loadRawConfigs(prefix: String, type: Class<T>, filename: String): T {
    val stream = Stream(File(filename).inputStream())
    val loader = PropertyConfigLoader(stream)
    val provider = ProxyConfigProvider(loader)
    return provider.bind(prefix, type)
}

/**
 * Loads ETH passwords. Lookup priority: command line args>environment variables>property file
 */
fun loadEthPasswords(prefix: String, filename: String, args: Array<String> = emptyArray()): EthereumPasswords {
    val ethPasswordsFromArgs = createEthPasswordsFromArgs(args)
    if (ethPasswordsFromArgs != null) {
        logger.info { "eth passwords configuration was taken from command line arguments" }
        return ethPasswordsFromArgs
    }
    val ethPasswordsFromEnv = createEthPasswordsFromEnv()
    if (ethPasswordsFromEnv != null) {
        logger.info { "eth passwords configuration was taken from env variables" }
        return ethPasswordsFromEnv
    }
    logger.info { "eth passwords configuration was taken from properties file" }
    return loadConfigs(prefix, EthereumPasswords::class.java, filename)
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
