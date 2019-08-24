/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.config

import com.github.kittinunf.result.Result
import com.jdiazcano.cfg4k.loaders.EnvironmentConfigLoader
import com.jdiazcano.cfg4k.loaders.PropertyConfigLoader
import com.jdiazcano.cfg4k.providers.OverrideConfigProvider
import com.jdiazcano.cfg4k.providers.ProxyConfigProvider
import com.jdiazcano.cfg4k.sources.ConfigSource
import mu.KLogging
import java.io.File
import java.io.IOException
import java.io.InputStream

//Environment variable that holds current application profile
const val PROFILE_ENV = "PROFILE"

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
interface IrohaCredentialRawConfig {
    // ID of Iroha account
    val accountId: String
    // Raw public key data in hex format
    val pubkey: String
    // Raw private key data in hex format
    val privkey: String
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
 * Returns default D3 config folder
 */
fun getConfigFolder() = System.getProperty("user.dir") + "/configs"

/**
 * Load configs from Java properties
 */
fun <T : Any> loadConfigs(
    prefix: String,
    type: Class<T>,
    filename: String
): Result<T, Exception> {
    return Result.of {
        val path = "${getConfigFolder()}${getProfiledConfigFileName(filename)}"
        logger.info { "Loading config from $path, prefix $prefix" }
        loadRawConfigs(prefix, type, path)
    }
}

/**
 * Load configs from Java properties located in 'resources' folder
 */
fun <T : Any> loadLocalConfigs(
    prefix: String,
    type: Class<T>,
    filename: String
): Result<T, Exception> {
    return Result.of {
        val path = getProfiledConfigFileName(filename)
        logger.info { "Loading local config from $path, prefix $prefix" }
        loadRawLocalConfigs(prefix, type, path)
    }
}

/**
 * Returns filename with profile postfix
 */
private fun getProfiledConfigFileName(filename: String): String {
    val profile = getProfile()
    val (file, extension) = filename.split(".")
    return "${file}_$profile.$extension"
}

class Stream(private val stream: InputStream) : ConfigSource {
    override fun read(): InputStream {
        return stream
    }
}

/**
 * Loads configs as is(no profiles).
 */
fun <T : Any> loadRawConfigs(
    prefix: String,
    type: Class<T>,
    filename: String
) = loadStreamConfig(prefix, type, filename) { File(filename).inputStream() }

/**
 * Loads configs from 'resources' folder as is(no profiles).
 */
fun <T : Any> loadRawLocalConfigs(
    prefix: String,
    type: Class<T>,
    filename: String
) = loadStreamConfig(prefix, type, filename) { Thread.currentThread().contextClassLoader.getResourceAsStream(filename) }

private fun <T : Any> loadStreamConfig(
    prefix: String,
    type: Class<T>,
    filename: String,
    streamLoader: (String) -> InputStream
): T {
    val envLoader = EnvironmentConfigLoader()
    val envProvider = ProxyConfigProvider(envLoader)
    return try {
        streamLoader(filename).use { inputStream ->
            val stream = Stream(inputStream)
            val configLoader = PropertyConfigLoader(stream)
            val provider = OverrideConfigProvider(
                envProvider,
                ProxyConfigProvider(configLoader)
            )
            provider.bind(prefix, type)
        }
    } catch (e: Exception) {
        logger.warn { "Couldn't open a file $filename. Trying to use only env variables." }
        envProvider.bind(prefix, type)
    }
}
