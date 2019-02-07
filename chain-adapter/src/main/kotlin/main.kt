@file:JvmName("ChainAdapterMain")

package chainadapter

import com.github.kittinunf.result.failure
import config.RMQConfig
import com.github.kittinunf.result.map
import config.getConfigFolder
import config.loadConfigs
import config.loadRawConfigs
import mu.KLogging


private val logger = KLogging().logger

fun main(args: Array<String>) {
    val rmqConfig = loadRawConfigs("rmq", RMQConfig::class.java, "${getConfigFolder()}/rmq.properties")
    val adapter = ChainAdapter(rmqConfig)
    adapter.run()
}
