package fee

import org.bitcoinj.core.Block
import org.bitcoinj.core.Transaction

const val BYTES_PER_INPUT = 180
const val BYTES_PER_OUTPUT = 34

/**
 * Returns fee rate of transaction
 */
fun Transaction.feeRate(): Int {
    this.fee?.let { fee ->
        return fee.value.div(this.getTxSize()).toInt()
    }
    return 0
}

/**
 * Returns fee rate of block
 */
fun Block.avgFeeRate(): Int {
    var sumFeeRate = 0L
    this.transactions!!.forEach { transaction ->
        sumFeeRate += transaction.feeRate()
    }
    return sumFeeRate.div(this.transactions!!.size).toInt()
}

// Computes fee size based on inputs
private fun getTxSizeInputs(inputs: Int) = inputs * BYTES_PER_INPUT

// Computes fee size based on outputs
private fun getTxSizeOutputs(outputs: Int) = outputs * BYTES_PER_OUTPUT

// Returns transaction size that depends on number of inputs and outputs
fun Transaction.getTxSize() = (getTxSizeInputs(this.inputs.size) + getTxSizeOutputs(this.outputs.size))

// Computes fee rate based on inputs and outputs
fun getTxFee(inputs: Int, outputs: Int, feeRate: Int) = (getTxSizeInputs(inputs) + getTxSizeOutputs(outputs)) * feeRate
