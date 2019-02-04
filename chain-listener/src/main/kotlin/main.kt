package chainlistener

import config.RMQConfig
import com.github.kittinunf.result.map
import config.loadConfigs
import model.IrohaCredential
import sidechain.iroha.util.ModelUtil
import com.rabbitmq.client.ConnectionFactory
import jp.co.soramitsu.iroha.java.IrohaAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KLogging


private val logger = KLogging().logger

fun main(args: Array<String>) {
    val rmqConfig = loadConfigs("rmq", RMQConfig::class.java, "rmq.properties", true).get()

    val rmqKeypair = ModelUtil.loadKeypair(
        rmqConfig.irohaCredential.pubkeyPath,
        rmqConfig.irohaCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    val withdrawalCredential = IrohaCredential(rmqConfig.irohaCredential.accountId, rmqKeypair)

    val irohaAPI = IrohaAPI(rmqConfig.iroha.hostname, rmqConfig.iroha.port)


    val factory = ConnectionFactory()
    factory.host = rmqConfig.host
    val conn = factory.newConnection()

    GlobalScope.launch {
        conn.use { connection ->
            connection.createChannel().use { channel ->
                channel.exchangeDeclare(rmqConfig.ethIrohaExchange, "fanout", true)
                val obs = ModelUtil.getBlockStreaming(irohaAPI, withdrawalCredential).map { observable ->
                    observable.map { response ->
                        logger.info { "New Iroha block arrived. Height ${response.blockResponse.block.blockV1.payload.height}" }
                        response.blockResponse.block
                    }

                }.get()
                logger.info { "Listening Iroha blocks" }
                obs.blockingSubscribe {
                    val message = it.toByteArray()
                    channel.basicPublish(rmqConfig.ethIrohaExchange, "", null, message)
                    println("Block pushed")

                }
            }
        }
    }


}