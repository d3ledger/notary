package com.d3.eth.token

import com.d3.commons.model.IrohaCredential
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumer
import com.d3.commons.sidechain.iroha.consumer.IrohaConsumerImpl
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import jp.co.soramitsu.iroha.java.IrohaAPI
import jp.co.soramitsu.iroha.java.Transaction
import mu.KLogging
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

/**
 * ERC20 tokens registration class. [IrohaCredential] is used to sign Iroha txs.
 */
class ERC20TokenRegistration(
    private val tokenRegistrationConfig: ERC20TokenRegistrationConfig,
    irohaCredential: IrohaCredential,
    irohaAPI: IrohaAPI
) {
    // For json serialization/deserialization
    private val moshi = Moshi.Builder().build()

    private val irohaConsumer = IrohaConsumerImpl(irohaCredential, irohaAPI)

    // Initiates process of ERC20 tokens registration
    fun init(): Result<Unit, Exception> {
        return registerTokensFromFile(
            tokenRegistrationConfig.ethAnchoredTokensFilePath,
            tokenRegistrationConfig.ethAnchoredTokenStorageAccount
        ).flatMap {
            registerTokensFromFile(
                tokenRegistrationConfig.irohaAnchoredTokensFilePath,
                tokenRegistrationConfig.irohaAnchoredTokenStorageAccount
            )
        }
    }

    /**
     * Reads ERC20 tokens from file and register them in account details of [storageAccountId].
     */
    private fun registerTokensFromFile(
        path: String,
        storageAccountId: String
    ): Result<Unit, Exception> {
        return readTokensFromFile(path)
            .flatMap { tokensToRegister ->
                if (tokensToRegister.isEmpty()) {
                    Result.of { logger.warn { "No ERC20 tokens to register" } }
                } else {
                    logger.info { "ERC20 tokens to register $tokensToRegister" }
                    registerERC20Tokens(
                        tokensToRegister,
                        storageAccountId,
                        irohaConsumer
                    ).map { Unit }
                }
            }
    }

    /**
     * Reads ERC20 tokens from json file
     * @param pathToTokensFile - path to file that holds tokens in json format
     * @return map of tokens (address -> token info)
     */
    private fun readTokensFromFile(pathToTokensFile: String): Result<Map<String, EthTokenInfo>, Exception> {
        return Result.of {
            val tokensJson = readFile(File(pathToTokensFile))
            val adapter = moshi.adapter<Map<String, EthTokenInfo>>(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    EthTokenInfo::class.java
                )
            )
            adapter.fromJson(tokensJson)!!
        }
    }

    /**
     * Reads content of file as a text
     * @param file - file to read
     * @return text content of file
     */
    private fun readFile(file: File): String {
        val content = StringBuilder()
        var lineBreak = ""
        FileInputStream(file).use { fis ->
            BufferedReader(InputStreamReader(fis)).use { reader ->
                while (true) {
                    val currentLine = reader.readLine() ?: break
                    content.append(lineBreak).append(currentLine)
                    lineBreak = "\n"
                }
            }
        }
        return content.toString()
    }

    /**
     * Registers ERC20 tokens in Iroha
     * @param tokens - map of tokens to register(address->token info
     * @param tokenStorageAccount - account that holds tokens
     * @param irohaConsumer - iroha network layer
     * @return hex representation of transaction hash
     */
    fun registerERC20Tokens(
        tokens: Map<String, EthTokenInfo>,
        tokenStorageAccount: String,
        irohaConsumer: IrohaConsumer
    ): Result<String, Exception> {
        return Result.of {
            var utx = Transaction.builder(irohaConsumer.creator)
            tokens.forEach { ethWallet, ethTokenInfo ->
                utx = utx.createAsset(
                    ethTokenInfo.name,
                    ethTokenInfo.domain,
                    ethTokenInfo.precision
                )
                utx = utx.setAccountDetail(
                    tokenStorageAccount,
                    ethWallet,
                    "${ethTokenInfo.name}#${ethTokenInfo.domain}"
                )
            }

            utx.build()
        }.flatMap { utx ->
            irohaConsumer.send(utx)
        }
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
