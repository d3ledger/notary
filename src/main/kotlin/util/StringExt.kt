package util

import java.util.*


private const val CHAR = "abcdefghijklmnopqrstuvwxyz"

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
    val res = StringBuilder()
    for (i in 0..len) {
        res.append(CHAR[Random().nextInt(CHAR.length)])
    }
    return res.toString()
}
