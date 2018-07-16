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
    val credentialsPassword: String
    val gasPrice: Long
    val gasLimit: Long
}

/**
 * Database configurations
 */
interface DatabaseConfig {
    val url: String
    val username: String
    val password: String
}

interface RefundConfig {
    val port: Int
    val endPointEth: String
}

/**
 * Load configs from Java properties
 */
fun <T : Any> loadConfigs(prefix: String, type: Class<T>): T {
    val loader = PropertyConfigLoader(ClasspathConfigSource("/defaults.properties"))
    val provider = ProxyConfigProvider(loader)
    return provider.bind(prefix, type)
}
