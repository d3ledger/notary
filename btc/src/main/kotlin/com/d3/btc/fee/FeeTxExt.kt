package com.d3.btc.fee

const val BYTES_PER_INPUT = 180
const val BYTES_PER_OUTPUT = 34

// Computes fee size based on inputs
private fun getTxSizeInputs(inputs: Int) = inputs * BYTES_PER_INPUT

// Computes fee size based on outputs
private fun getTxSizeOutputs(outputs: Int) = outputs * BYTES_PER_OUTPUT

// Computes fee rate based on inputs and outputs
fun getTxFee(inputs: Int, outputs: Int, feeRate: Int) = (getTxSizeInputs(inputs) + getTxSizeOutputs(outputs)) * feeRate
