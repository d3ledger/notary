package com.d3.commons.util

import java.io.File
import java.io.IOException

/**
 * Creates folder with all parent sub-folders.
 * @param folderPath - path of folder to create
 * @throws IOException, if it's impossible to create folder
 */
fun createFolderIfDoesntExist(folderPath: String) {
    val folder = File(folderPath)
    if (!folder.exists() && !File(folderPath).mkdirs()) {
        throw IOException("Cannot create folder $folderPath")
    }
}
