package sidechain.iroha

import config.RMQConfig
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import mu.KLogging
import sidechain.ChainListener
import sidechain.iroha.util.ModelUtil
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlin.test.assertNotNull


/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener(
    private val irohaAPI: IrohaAPI,
    private val credential: IrohaCredential,
    private val rmqConfig: RMQConfig? = null,
    private val irohaQueue: String? = null
) : ChainListener<Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit>> {

    constructor(
        irohaHost: String,
        irohaPort: Int,
        credential: IrohaCredential,
        rmqConfig: RMQConfig? = null,
        irohaQueue: String? = null
    ) : this(IrohaAPI(irohaHost, irohaPort), credential, rmqConfig, irohaQueue)

    fun getIrohaBlockObservable(): Result<Observable<iroha.protocol.BlockOuterClass.Block>, Exception> {
        return ModelUtil.getBlockStreaming(irohaAPI, credential).map { observable ->
            observable.map { response ->
                logger.info { "New Iroha block arrived. Height ${response.blockResponse.block.blockV1.payload.height}" }
                response.blockResponse.block
            }
        }

    }

    /**
     * Returns an observable that emits a new block every time it gets it from Iroha
     */
    override fun getBlockObservable(autoAck: Boolean): Result<Observable<Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit>>, Exception> {
        assertNotNull(rmqConfig)
        assertNotNull(irohaQueue)
        val factory = ConnectionFactory()
        factory.host = rmqConfig.host
        val conn = factory.newConnection()

        val channel = conn.createChannel()

        val source = PublishSubject.create<Delivery>()
        val obs: Observable<Delivery> = source
        channel.queueDeclare(irohaQueue, true, false, false, null)
        val deliverCallback = { consumerTag: String, delivery: Delivery ->
            source.onNext(delivery)
        }
        channel.exchangeDeclare(rmqConfig.ethIrohaExchange, "fanout")
        channel.queueDeclare(irohaQueue, true, false, false, null)
        channel.queueBind(irohaQueue, rmqConfig.ethIrohaExchange, "")
        channel.basicConsume(irohaQueue, autoAck, deliverCallback, { consumerTag -> })


        logger.info { "On subscribe to Iroha chain" }
        return Result.of {
            obs.map { delivery ->
                val block = iroha.protocol.BlockOuterClass.Block.parseFrom(delivery.body)
                logger.info { "New Iroha block arrived. Height ${block.blockV1.payload.height}" }
                Pair(block, {
                    if (!autoAck)
                        channel.basicAck(delivery.envelope.deliveryTag, false)
                })
            }
        }

    }

    /**
     * @return a block as soon as it is committed to iroha
     */
    override suspend fun getBlock(): Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit> {
        return getBlockObservable().get().blockingFirst()
    }

    override fun close() {
        irohaAPI.close()
    }

    /**
     * Logger
     */
    companion object : KLogging()
}
