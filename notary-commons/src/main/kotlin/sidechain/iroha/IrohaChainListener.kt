package sidechain.iroha

import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import jp.co.soramitsu.iroha.java.IrohaAPI
import model.IrohaCredential
import mu.KLogging
import sidechain.ChainListener
import sidechain.iroha.util.ModelUtil
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.*
import java.nio.charset.Charset
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

val QUEUE_NAME = "iroha_blocks"
val HOST = "51.15.62.100"

/**
 * Dummy implementation of [ChainListener] with effective dependencies
 */
class IrohaChainListener(
    private val irohaAPI: IrohaAPI,
    private val credential: IrohaCredential
) : ChainListener<Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit>> {

    constructor(
        irohaHost: String,
        irohaPort: Int,
        credential: IrohaCredential
    ) : this(IrohaAPI(irohaHost, irohaPort), credential)

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
    override fun getBlockObservable(): Result<Observable<Pair<iroha.protocol.BlockOuterClass.Block, () -> Unit>>, Exception> {
        val factory = ConnectionFactory()
        factory.host = HOST
        val conn = factory.newConnection()

        val channel = conn.createChannel()

        val source = PublishSubject.create<Delivery>()
        val obs: Observable<Delivery> = source
        channel.queueDeclare(QUEUE_NAME, true, false, false, null)
        val deliverCallback = { consumerTag: String, delivery: Delivery ->
            val message = delivery.getBody()
            println(" [x] Received '$message'")
            source.onNext(delivery)

        }
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, { consumerTag -> })


        logger.info { "On subscribe to Iroha chain" }
        return Result.of {
            obs.map { delivery ->
                val block = iroha.protocol.BlockOuterClass.Block.parseFrom(delivery.body)
                logger.info { "New Iroha block arrived. Height ${block.blockV1.payload.height}" }
                Pair(block, {
                    channel.basicAck(delivery.envelope.deliveryTag, false)
                })
            }
        }
//            return ModelUtil.getBlockStreaming(irohaAPI, credential).map { observable ->
//                observable.map { response ->
//                    logger.info { "New Iroha block arrived. Height ${response.blockResponse.block.blockV1.payload.height}" }
//                    response.blockResponse.block
//                }
//            }

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
