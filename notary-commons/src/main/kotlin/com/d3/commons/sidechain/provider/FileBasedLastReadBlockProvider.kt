/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.provider

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*

/**
 * File based last processed Iroha block reader
 */
class FileBasedLastReadBlockProvider(private val lastReadBlockFilePath: String) :
    LastReadBlockProvider {

    /**
     * Returns last processed block
     * Value is read from file
     */
    @Synchronized
    override fun getLastBlockHeight(): Long {
        Scanner(File(lastReadBlockFilePath)).use { scanner ->
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
        FileWriter(File(lastReadBlockFilePath)).use { fileWriter ->
            BufferedWriter(fileWriter).use { writer ->
                writer.write(height.toString())
            }
        }
    }
}
