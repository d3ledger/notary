package util

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
