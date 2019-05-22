package integration.helper

import com.d3.commons.sidechain.iroha.util.ModelUtil
import com.google.protobuf.util.JsonFormat
import iroha.protocol.BlockOuterClass
import jp.co.soramitsu.iroha.testcontainers.IrohaContainer
import jp.co.soramitsu.iroha.testcontainers.PeerConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.io.Closeable
import java.io.File


const val DEFAULT_RMQ_PORT = 5672

/**
 * Helper that is used to start Iroha, create containers, etc
 */
class ContainerHelper : Closeable {

    val userDir = System.getProperty("user.dir")!!

    private val peerKeyPair =
        ModelUtil.loadKeypair(
            "$userDir/deploy/iroha/keys/node0.pub",
            "$userDir/deploy/iroha/keys/node0.priv"
        ).get()

    val irohaContainer =
        IrohaContainer()
            .withPeerConfig(getPeerConfig())
            .withLogger(null)!! // turn of nasty Iroha logs

    val rmqContainer =
        KGenericContainer("rabbitmq:3-management").withExposedPorts(DEFAULT_RMQ_PORT)

    /**
     * Creates service docker container based on [dockerFile]
     * @param jarFile - path to jar file that will be used to run service
     * @param dockerFile - path to docker file that will be used to create containers
     * @return container
     */
    fun createContainer(jarFile: String, dockerFile: String): KGenericContainerImage {
        return KGenericContainerImage(
            ImageFromDockerfile()
                .withFileFromFile(jarFile, File(jarFile))
                .withFileFromFile("Dockerfile", File(dockerFile)).withBuildArg("JAR_FILE", jarFile)
        ).withLogConsumer { outputFrame -> print(outputFrame.utf8String) }.withNetworkMode("host")
    }

    /**
     * Returns Iroha peer config
     */
    private fun getPeerConfig(): PeerConfig {
        val builder = BlockOuterClass.Block.newBuilder()
        JsonFormat.parser().merge(File("$userDir/deploy/iroha/genesis.block").readText(), builder)
        val config = PeerConfig.builder()
            .genesisBlock(builder.build())
            .build()
        config.withPeerKeyPair(peerKeyPair)
        return config
    }

    /**
     * Checks if service is healthy
     * @param serviceContainer - container of service to check
     * @param healthCheckPort - port of health check service
     * @return true if healthy
     */
    fun isServiceHealthy(serviceContainer: KGenericContainerImage) = serviceContainer.isRunning

    /**
     * Cheks if service is dead
     * @param serviceContainer - container of service to check
     * @return true if dead
     */
    fun isServiceDead(serviceContainer: KGenericContainerImage) = !serviceContainer.isRunning

    override fun close() {
        if (irohaContainer.irohaDockerContainer != null
            && irohaContainer.irohaDockerContainer.isRunning()
        ) {
            irohaContainer.stop()
        }
        if (rmqContainer.isRunning) {
            rmqContainer.close()
        }
    }
}

/**
 * The GenericContainer class is not very friendly to Kotlin.
 * So the following classes were created as a workaround.
 */
class KGenericContainerImage(image: ImageFromDockerfile) : GenericContainer<KGenericContainerImage>(image)

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
