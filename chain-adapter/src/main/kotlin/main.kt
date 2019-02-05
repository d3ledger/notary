@file:JvmName("ChainAdapterMain")

package chainadapter

import com.github.kittinunf.result.failure
import config.RMQConfig
import com.github.kittinunf.result.map
import config.loadConfigs
import mu.KLogging


private val logger = KLogging().logger

fun main(args: Array<String>) {
    loadConfigs("rmq", RMQConfig::class.java, "/rmq.properties", true)
        .map { rmqConfig ->
            val adapter = ChainAdapter(rmqConfig)
            adapter.run()
        }
        .failure { ex ->
            logger.error("Cannot run chain adapter", ex)
            System.exit(1)
        }


}