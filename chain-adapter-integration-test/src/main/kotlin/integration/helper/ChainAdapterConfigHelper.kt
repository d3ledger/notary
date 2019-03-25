/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package integration.helper

import com.d3.commons.config.RMQConfig
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadRawConfigs
import com.d3.commons.util.getRandomString
import java.io.File
import java.io.IOException

// Folder for chain-adapter test files(last read block file and etc)
private const val LAST_READ_BLOCK_TEST_FOLDER = "deploy/chain-adapter/tests"

class ChainAdapterConfigHelper {

    /**
     * Creates RabbitMQ config
     */
    fun createRmqConfig(): RMQConfig {
        val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")
        return object : RMQConfig {
            override val host = rmqConfig.host
            override val irohaExchange = String.getRandomString(5)
            override val irohaCredential = rmqConfig.irohaCredential
            override val iroha = rmqConfig.iroha
            override val lastReadBlockFilePath = createTestLastReadBlockFile()
        }
    }

    /**
     * Creates randomly named file for last read block height storage
     */
    private fun createTestLastReadBlockFile(): String {
        val file = File("$LAST_READ_BLOCK_TEST_FOLDER/last_block_${String.getRandomString(5)}.txt")
        val folder = File(file.parentFile.absolutePath)
        if (!folder.exists() && !folder.mkdirs()) {
            throw IOException("Cannot create chain-adapter test folder")
        }
        if (!file.createNewFile()) {
            throw IOException("Cannot create file for last read block storage")
        }
        return file.absolutePath
    }

}
