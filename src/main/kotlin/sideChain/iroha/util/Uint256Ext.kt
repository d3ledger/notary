package sideChain.iroha.util

import iroha.protocol.Primitive.uint256
import java.math.BigInteger

/**
 * Extension function that converts [iroha.protocol.Primitive.uint256] to [BigInteger].
 */
fun uint256.toBigInteger() = BigInteger.valueOf(this.first).shiftLeft(256)
    .add(BigInteger.valueOf(this.second)).shiftLeft(256)
    .add(BigInteger.valueOf(this.third)).shiftLeft(256)
    .add(BigInteger.valueOf(this.fourth))
