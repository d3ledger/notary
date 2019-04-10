package com.d3.commons.util

import com.google.gson.GsonBuilder
import java.util.*
import javax.xml.bind.DatatypeConverter


private const val CHAR = "abcdefghijklmnopqrstuvwxyz"

//Iroha can't stand unescaped quote symbols
private const val IROHA_FRIENDLY_QUOTE = "\\\""

//JSON formatter
private val gson = GsonBuilder().setPrettyPrinting().create()

/**
 * Extension function to convert hexidecimal string to text
 */
fun String.hexToAscii(): String {
    val output = StringBuilder("")

    var i = 0
    while (i < this.length) {
        val str = this.substring(i, i + 2)
        output.append(Integer.parseInt(str, 16).toChar())
        i += 2
    }

    return output.toString()
}

/** Returns random string of [len] characters */
fun String.Companion.getRandomString(len: Int): String {
    val random = Random()
    val res = StringBuilder()
    for (i in 0..len) {
        res.append(CHAR[random.nextInt(CHAR.length)])
    }
    return res.toString()
}

fun String.Companion.getRandomId(): String {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 32)
}

// Transforms byte array into hex string
fun String.Companion.hex(bytes: ByteArray): String {
    return DatatypeConverter.printHexBinary(bytes)
}

// Transforms hex string into byte array
fun String.Companion.unHex(s: String): ByteArray {
    return DatatypeConverter.parseHexBinary(s)
}

/**
 * Escapes symbols reserved in JSON so it can be used in Iroha
 */
fun String.irohaEscape(): String {
    return this.replace("\"", IROHA_FRIENDLY_QUOTE)
        .replace("\n", "\\n")
}

// Reverse changes of 'irohaEscape'
fun String.irohaUnEscape(): String {
    return this.replace(IROHA_FRIENDLY_QUOTE, "\"")
}

//TODO can we get rid of klaxon and moshi? Gson is much easier thing to use.
// Turns any object to JSON
fun String.Companion.toJson(obj: Any) = gson.toJson(obj)
