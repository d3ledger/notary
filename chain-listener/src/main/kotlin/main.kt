import config.loadConfigs
import model.IrohaCredential
import notary.eth.EthNotaryConfig
import sidechain.iroha.IrohaChainListener
import sidechain.iroha.util.ModelUtil
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.*
import java.nio.charset.Charset
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

val QUEUE_NAME = "iroha_blocks"
val HOST = "51.15.62.100"

typealias Message = Pair<ByteArray, Delivery>

fun main(args: Array<String>) {
    val notaryConfig = loadConfigs("eth-notary", EthNotaryConfig::class.java, "/eth/notary.properties").get()
    val notaryKeypair = ModelUtil.loadKeypair(
        notaryConfig.notaryCredential.pubkeyPath,
        notaryConfig.notaryCredential.privkeyPath
    ).fold({ keypair -> keypair }, { ex -> throw ex })

    val notaryCredential = IrohaCredential(notaryConfig.notaryCredential.accountId, notaryKeypair)

    val listener = IrohaChainListener(
        notaryConfig.iroha.hostname,
        notaryConfig.iroha.port,
        notaryCredential
    )

    val factory = ConnectionFactory()
    factory.host = HOST
    val conn = factory.newConnection()

    GlobalScope.launch {
        conn.use { connection ->
            connection.createChannel().use { channel ->
                channel.queueDeclare(QUEUE_NAME, true, false, false, null)
                println(channel.connection.isOpen)
                listener.getBlockObservable().get()
                    .blockingSubscribe {
                        val message = it.toByteArray()
                        channel.basicPublish("", QUEUE_NAME, null, message)
                        println(" [x] Sent '$message'")

                    }
            }
        }
    }

    while(true);
    val channel = conn.createChannel()

    val source = PublishSubject.create<Delivery>()
    val obs: Observable<Delivery> = source
    GlobalScope.launch {
        obs.subscribe { delivery ->
            println(delivery.body)
            channel.basicAck(delivery.envelope.deliveryTag, false)
        }
    }

    runBlocking {
        channel.queueDeclare(QUEUE_NAME, true, false, false, null)
        val deliverCallback = { consumerTag: String, delivery: Delivery ->
            val message = delivery.getBody()
            println(" [x] Received '$message'")
            source.onNext(delivery)

        }
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, { consumerTag -> })
        while (true) { delay(1000) }


    }


}