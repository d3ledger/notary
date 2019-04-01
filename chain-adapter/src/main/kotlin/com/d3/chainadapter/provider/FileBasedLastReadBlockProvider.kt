package com.d3.chainadapter.provider

import com.d3.commons.config.RMQConfig
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*


/**
 * File based last processed Iroha block reader
 */
class FileBasedLastReadBlockProvider(private val rmqConfig: RMQConfig) : LastReadBlockProvider {

    /**
     * Returns last processed block
     * Value is read from file
     */
    @Synchronized
    override fun getLastBlockHeight(): Long {
        Scanner(File(rmqConfig.lastReadBlockFilePath)).use { scanner ->
            return if (scanner.hasNextLine()) {
                scanner.next().toLong()
            } else {
                0
            }
        }
    }

    /**
     * Save last block height in file
     * @param height - height of block that will be saved
     */
    @Synchronized
    override fun saveLastBlockHeight(height: Long) {
        FileWriter(File(rmqConfig.lastReadBlockFilePath)).use { fileWriter ->
            BufferedWriter(fileWriter).use { writer ->
                writer.write(height.toString())
            }
        }
    }
}
