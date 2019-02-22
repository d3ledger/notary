@file:JvmName("ChainAdapterMain")

package com.d3.chainadapter

import config.RMQConfig
import config.getConfigFolder
import config.loadRawConfigs

fun main(args: Array<String>) {
    val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")
    val adapter = ChainAdapter(rmqConfig)
    adapter.run()
}
