@file:JvmName("ChainAdapterMain")

package com.d3.chainadapter

import com.d3.commons.config.RMQConfig
import com.d3.commons.config.getConfigFolder
import com.d3.commons.config.loadRawConfigs

fun main(args: Array<String>) {
    val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")
    val adapter = ChainAdapter(rmqConfig)
    adapter.run()
}
