package sidechain.iroha

import com.github.kittinunf.result.Result
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import com.rabbitmq.client.GetResponse
import config.RMQConfig
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import sidechain.ChainListener
import java.util.concurrent.ExecutorService

/**
 * Rabbit MQ based implementation of [ChainListener]
 */
class ReliableIrohaChainListener(
    private val rmqConfig: RMQConfig,
    private val irohaQueue: String,
    private val consumerExecutorService: ExecutorService? = null
) : ChainListener<Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit>> {

    private val factory = ConnectionFactory()

    private val conn by lazy {
        factory.host = rmqConfig.host
        if (consumerExecutorService != null) {
            factory.newConnection(consumerExecutorService)
        } else {
            factory.newConnection()
        }

    }

    private val channel by lazy { conn.createChannel() }

    private var consumerTag: String? = null

    init {
        channel.exchangeDeclare(rmqConfig.irohaExchange, "fanout", true)
        channel.queueDeclare(irohaQueue, true, false, false, null)
        channel.queueBind(irohaQueue, rmqConfig.irohaExchange, "")
        channel.basicQos(1)
    }


    /**
     * Returns an observable that emits a new block every time it gets it from Iroha
     */
    override fun getBlockObservable(autoAck: Boolean): Result<Observable<Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit>>, Exception> {

        val source = PublishSubject.create<Delivery>()
        val deliverCallback = { consumerTag: String, delivery: Delivery ->
            // This code is executed inside consumerExecutorService
            source.onNext(delivery)
        }
        val obs: Observable<Delivery> = source.doOnSubscribe {
            consumerTag = channel.basicConsume(irohaQueue, autoAck, deliverCallback, { _ -> })
        }
        logger.info { "On subscribe to Iroha chain" }
        return Result.of {
            obs.map { delivery ->
                val block = iroha.protocol.BlockOuterClass.Block.parseFrom(delivery.body)
                logger.info { "New Iroha block arrived. Height ${block.blockV1.payload.height}" }
                Pair(block, {
                    if (!autoAck) {
                        channel.basicAck(delivery.envelope.deliveryTag, false)
                        logger.info { "Iroha block delivery confirmed" }
                    }
                })
            }
        }
    }

    /**
     * @return a block as soon as it is committed to iroha
     */
    override suspend fun getBlock(autoAck: Boolean): Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit> {
        var resp: GetResponse?
        do {
            Thread.sleep(10L)
            resp = channel.basicGet(irohaQueue, autoAck)
        } while (resp == null)

        val block = iroha.protocol.BlockOuterClass.Block.parseFrom(resp.body)
        return Pair(block, {
            if (!autoAck)
                channel.basicAck(resp.envelope.deliveryTag, false)
        })
    }

    fun purge() {
        channel.queuePurge(irohaQueue)
    }

    override fun close() {
        consumerTag?.let {
            channel.basicCancel(it)
        }
        consumerExecutorService?.shutdownNow()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
