package util

import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringExtTest {

    @Test
    fun hexToAsciiTest() {
        assertEquals("hello hex world", "68656c6c6f2068657820776f726c64".hexToAscii())
    }

    @Test
    fun test() {
        val host = "127.0.0.1"
        val port = 50051

        val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
        println(channel.getState(true))
        repeat(100) {

            println(channel.getState(false))
            runBlocking {

                delay(10)
            }
        }
        println(channel.getState(true))
        println(channel.getState(true))
        println(channel.getState(true))


    }

}
