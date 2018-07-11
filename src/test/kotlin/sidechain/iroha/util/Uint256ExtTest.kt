package sidechain.iroha.util

import org.junit.jupiter.api.Test
import java.math.BigInteger
import org.junit.jupiter.api.Assertions.assertEquals

class Uint256ExtTest {

    /**
     * @given uint256 protobuf builder
     * @when set fourth part
     * @then the converter toBigInteger is correct
     */
    @Test
    fun test123() {
        val number = iroha.protocol.Primitive.uint256.newBuilder()
            .setFirst(0)
            .setSecond(0)
            .setThird(0)
            .setFourth(123)
            .build()

        assertEquals(BigInteger.valueOf(123), number.toBigInteger())
    }

    /**
     * @given uint256 protobuf builder
     * @when set first part
     * @then the converter toBigInteger is correct
     */
    @Test
    fun testFirst() {
        val number = iroha.protocol.Primitive.uint256.newBuilder()
            .setFirst(1)
            .setSecond(0)
            .setThird(0)
            .setFourth(0)
            .build()

        assertEquals(BigInteger.valueOf(2).pow(192), number.toBigInteger())
    }

    /**
     * @given uint256 protobuf builder
     * @when set second part
     * @then the converter toBigInteger is correct
     */
    @Test
    fun testSecond() {
        val number = iroha.protocol.Primitive.uint256.newBuilder()
            .setFirst(0)
            .setSecond(1)
            .setThird(0)
            .setFourth(0)
            .build()

        assertEquals(BigInteger.valueOf(2).pow(128), number.toBigInteger())
    }

    /**
     * @given uint256 protobuf builder
     * @when set third part
     * @then the converter toBigInteger is correct
     */
    @Test
    fun testThird() {
        val number = iroha.protocol.Primitive.uint256.newBuilder()
            .setFirst(0)
            .setSecond(0)
            .setThird(1)
            .setFourth(0)
            .build()

        assertEquals(BigInteger.valueOf(2).pow(64), number.toBigInteger())
    }

}
