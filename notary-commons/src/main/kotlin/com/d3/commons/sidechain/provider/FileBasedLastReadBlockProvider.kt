/*
 * Copyright D3 Ledger, Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.d3.commons.sidechain.provider

import com.d3.commons.util.createFolderIfDoesntExist
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.math.BigInteger
import java.util.*

/**
 * File based last processed Iroha block reader
 */
class FileBasedLastReadBlockProvider(private val lastReadBlockFilePath: String) :
    LastReadBlockProvider {

    init {
        createLastReadBlockFile(lastReadBlockFilePath)
    }

    /**
     * Creates last read block file
     * @param lastReadBlockFilePath - path to the file
     */
    private fun createLastReadBlockFile(lastReadBlockFilePath: String) {
        val file = File(lastReadBlockFilePath)
        if (file.exists()) {
            //No need to create
            return
        }
        createFolderIfDoesntExist(file.parentFile.absolutePath)
        if (!file.createNewFile()) {
            throw IOException("Cannot create file for last read block storage")
        }
    }

    /**
     * Returns last processed block
     * Value is read from file
     */
    @Synchronized
    override fun getLastBlockHeight(): BigInteger {
        Scanner(File(lastReadBlockFilePath)).use { scanner ->
            return if (scanner.hasNextLine()) {
                scanner.next().toBigInteger()
            } else {
                BigInteger.ZERO
            }
        }
    }

    /**
     * Save last block height in file
     * @param height - height of block that will be saved
     */
    @Synchronized
    override fun saveLastBlockHeight(height: BigInteger) {
        FileWriter(File(lastReadBlockFilePath)).use { fileWriter ->
            BufferedWriter(fileWriter).use { writer ->
                writer.write(height.toString())
            }
        }
    }
}
