package token

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import jp.co.soramitsu.iroha.Keypair
import model.IrohaCredential
import mu.KLogging
import provider.eth.EthTokenInfo
import provider.eth.EthTokensProviderImpl
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

//ERC20 tokens registration class
class ERC20TokenRegistration(
    credential: IrohaCredential,
    private val tokenRegistrationConfig: ERC20TokenRegistrationConfig
) {

    //For json serialization/deserialization
    private val moshi = Moshi.Builder().build()

    private val ethTokensProvider = EthTokensProviderImpl(
        tokenRegistrationConfig.iroha,
        credential,
        tokenRegistrationConfig.tokenStorageAccount,
        tokenRegistrationConfig.tokenCreatorAccount.accountId
    )

    //Initiates process of ERC20 tokens registration
    fun init(): Result<Unit, Exception> {
        //It takes file full of tokens to be registered and registers it in Iroha
        return readTokensFromFile(tokenRegistrationConfig.tokensFilePath)
            .flatMap { tokensToRegister ->
                if (tokensToRegister.isEmpty()) {
                    Result.of { logger.warn { "No ERC20 tokens to register" } }
                } else {
                    ethTokensProvider.addTokens(tokensToRegister)
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
     * Logger
     */
    companion object : KLogging()
}
