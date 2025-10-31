package io.github.rudikone.avroschemawizardplugin

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@Suppress("UtilityClassWithPublicConstructor")
abstract class BaseTaskTest {
    companion object {
        private val network = Network.newNetwork()

        val kafkaContainer: KafkaContainer =
            KafkaContainer("apache/kafka-native:4.0.1")
                .withNetwork(network)
                .withListener("kafka:19092")

        private val schemaRegistryContainer =
            GenericContainer(DockerImageName.parse("confluentinc/cp-schema-registry:8.1.0"))
                .dependsOn(kafkaContainer)
                .withExposedPorts(8085)
                .withNetworkAliases("schemaregistry")
                .withNetwork(network)
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:19092")
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8085")
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schemaregistry")
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL", "PLAINTEXT")
                .waitingFor(Wait.forHttp("/subjects"))
                .withStartupTimeout(Duration.ofSeconds(120))

        init {
            Startables.deepStart(kafkaContainer, schemaRegistryContainer).join()
        }

        @JvmStatic
        protected val schemaRegistryUrl =
            "http://${schemaRegistryContainer.host}:${schemaRegistryContainer.getMappedPort(8085)}"
    }
}
