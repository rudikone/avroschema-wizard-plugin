package io.github.rudikone.avroschemawizardplugin

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName

@Suppress("UtilityClassWithPublicConstructor")
abstract class BaseTaskTest {
    companion object {
        val kafkaContainer: KafkaContainer =
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.1"))
                .withKraft()
                .withNetwork(Network.newNetwork())
                .withNetworkAliases("kafka")
                .withExposedPorts(9092, 9093)
                .waitingFor(Wait.forListeningPort())

        private val schemaRegistryContainer: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("confluentinc/cp-schema-registry:7.5.1"))
                .withNetwork(kafkaContainer.network)
                .withNetworkAliases("schema-registry")
                .withExposedPorts(8081)
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "localhost")
                .withEnv(
                    "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                    "PLAINTEXT://${kafkaContainer.networkAliases[0]}:9092",
                ).withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .waitingFor(Wait.forHealthcheck())
                .waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
                .dependsOn(kafkaContainer)

        init {
            Startables.deepStart(kafkaContainer, schemaRegistryContainer).join()
        }

        @JvmStatic
        protected val schemaRegistryUrl = "http://localhost:${schemaRegistryContainer.getMappedPort(8081)}"
    }
}
