package config

import com.jdiazcano.cfg4k.loaders.PropertyConfigLoader
import com.jdiazcano.cfg4k.providers.ProxyConfigProvider
import com.jdiazcano.cfg4k.sources.ClasspathConfigSource

/**
 * Iroha configurations
 */
interface IrohaConfig {
    val hostname: String
    val port: Int
    val creator: String
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
 * Ethereum passwords
 */
interface EthereumPasswords {
    val credentialsPassword: String
}

/**
 * Database configurations
 */
interface DatabaseConfig {
    val url: String
    val username: String
    val password: String
}

/**
 * Bitcoin config
 */
interface BitcoinConfig {
    val url: String
}

/**
 * Load configs from Java properties
 */
fun <T : Any> loadConfigs(prefix: String, type: Class<T>, filename: String = "/defaults.properties"): T {
    val loader = PropertyConfigLoader(ClasspathConfigSource(filename))
    val provider = ProxyConfigProvider(loader)
    return provider.bind(prefix, type)
}
