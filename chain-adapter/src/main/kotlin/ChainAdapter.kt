package chainadapter

import com.github.kittinunf.result.map
import com.rabbitmq.client.ConnectionFactory
import config.RMQConfig
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import mu.KLogging
import sidechain.iroha.util.ModelUtil

class ChainAdapter(private val rmqConfig: RMQConfig) {

    companion object : KLogging()

    fun run() {
        val rmqKeypair = ModelUtil.loadKeypair(
            rmqConfig.irohaCredential.pubkeyPath,
            rmqConfig.irohaCredential.privkeyPath
        ).fold({ keypair -> keypair }, { ex -> throw ex })

        val irohaCredential = IrohaCredential(rmqConfig.irohaCredential.accountId, rmqKeypair)

        val irohaAPI = IrohaAPI(rmqConfig.iroha.hostname, rmqConfig.iroha.port)

        val factory = ConnectionFactory()
        factory.host = rmqConfig.host
        val conn = factory.newConnection()

        conn.use { connection ->
            connection.createChannel().use { channel ->
                channel.exchangeDeclare(rmqConfig.ethIrohaExchange, "fanout", true)
                ModelUtil.getBlockStreaming(irohaAPI, irohaCredential).map { observable ->
                    observable.map { response ->
                        logger.info { "New Iroha block arrived. Height ${response.blockResponse.block.blockV1.payload.height}" }
                        response.blockResponse.block
                    }

                }.map { obs ->
                    logger.info { "Listening Iroha blocks" }
                    obs.blockingSubscribe {
                        val message = it.toByteArray()
                        channel.basicPublish(rmqConfig.ethIrohaExchange, "", null, message)
                        logger.info { "Block pushed" }

                    }
                }

            }
        }
    }
}
