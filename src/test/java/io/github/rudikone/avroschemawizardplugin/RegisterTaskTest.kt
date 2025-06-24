package io.github.rudikone.avroschemawizardplugin

import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator.addAvroFiles
import io.github.rudikone.avroschemawizardplugin.testutils.SimpleProject
import io.github.rudikone.avroschemawizardplugin.testutils.avroSchemaWizard
import io.github.rudikone.avroschemawizardplugin.testutils.exampleProtocol
import io.github.rudikone.avroschemawizardplugin.testutils.exampleSchema
import io.github.rudikone.avroschemawizardplugin.testutils.kotlinJvm
import io.github.rudikone.avroschemawizardplugin.testutils.randomString
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.lifecycle.Startables
import org.testcontainers.utility.DockerImageName

@Suppress("MaxLineLength", "ktlint:standard:max-line-length")
class RegisterTaskTest {
    @ParameterizedTest()
    @ValueSource(strings = ["1.7.0", "1.7.20", "1.8.0", "1.8.20", "1.9.0", "1.9.20", "2.0.0", "2.0.20", "2.1.0"])
    fun `schema is registered from schema and protocol`(kotlinVersion: String) {
        val firstTopic = randomString()
        val secondTopic = randomString()

        val exampleProtocolFile = exampleProtocol()
        val exampleSchemaFile = exampleSchema()

        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$firstTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        protocol.set("ExampleProtocol")
                        schema.set("FirstExampleRecordFromProtocol")
                    }
                    topic("$secondTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("Example")
                    }
                }
            }
            """.trimIndent()

        val testProject =
            SimpleProject(
                avroWizardConfig = avroWizardConfig,
                plugins = mutableListOf(kotlinJvm(version = kotlinVersion), avroSchemaWizard()),
            )
        val testProjectDir = ProjectDirGenerator.generate(testProject)
        testProjectDir.addAvroFiles(exampleProtocolFile, exampleSchemaFile)

        val buildResult =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withEnvironment(System.getenv())
                .withArguments(listOf("--build-cache", REGISTER_TASK_NAME, "-i", "-s"))
                .forwardOutput()
                .build()
                .output

        assertTrue {
            buildResult.contains(
                Regex("Registered FirstExampleRecordFromProtocol with id: \\d+ under subject $firstTopic-value"),
            )
            buildResult.contains(Regex("Registered Example with id: \\d+ under subject $secondTopic-value"))
        }
    }

    @Test
    fun `schema is registered by TopicNameStrategy, RecordNameStrategy and TopicRecordNameStrategy`() {
        val firstTopic = randomString()
        val secondTopic = randomString()
        val thirdTopic = randomString()

        val exampleProtocolFile = exampleProtocol()
        val exampleSchemaFile = exampleSchema()

        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$firstTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        protocol.set("ExampleProtocol")
                        schema.set("FirstExampleRecordFromProtocol")
                        subjectNameStrategy.set("TopicNameStrategy")
                    }
                    topic("$secondTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        protocol.set("ExampleProtocol")
                        schema.set("SecondExampleRecordFromProtocol")
                        subjectNameStrategy.set("RecordNameStrategy")
                    }
                    topic("$thirdTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("Example")
                        subjectNameStrategy.set("TopicRecordNameStrategy")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(testProject)
        testProjectDir.addAvroFiles(exampleProtocolFile, exampleSchemaFile)

        val buildResult =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withEnvironment(System.getenv())
                .withArguments(listOf("--build-cache", REGISTER_TASK_NAME, "-i", "-s"))
                .forwardOutput()
                .build()
                .output

        assertTrue {
            buildResult.contains(
                Regex("Registered FirstExampleRecordFromProtocol with id: \\d+ under subject $firstTopic-value"),
            )
            buildResult.contains(
                Regex(
                    "Registered SecondExampleRecordFromProtocol with id: \\d+ under subject ru.rudikov.example.SecondExampleRecordFromProtocol",
                ),
            )
            buildResult.contains(
                Regex("Registered Example with id: \\d+ under subject $thirdTopic-ru.rudikov.example.ExampleRecordFromSchema"),
            )
        }
    }

    @Test
    fun `file not found exception thrown when avro file in resources is missing`() {
        val topic = randomString()
        val randomSchema = "Random"

        val exampleSchemaFile = exampleSchema()

        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$topic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("$randomSchema")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(testProject)
        testProjectDir.addAvroFiles(exampleSchemaFile)

        val buildResult =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withEnvironment(System.getenv())
                .withArguments(listOf("--build-cache", REGISTER_TASK_NAME, "-i", "-s"))
                .forwardOutput()
                .build()
                .output

        assertTrue {
            buildResult.contains("Failed register $randomSchema for $topic")
            buildResult.contains("File $randomSchema not found!")
        }
    }

    @Test
    fun `unsupported subject name strategy exception thrown`() {
        val topic = randomString()
        val schema = "Example"

        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$topic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("$schema")
                        subjectNameStrategy.set("Unknown")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(testProject)

        val buildResult =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withEnvironment(System.getenv())
                .withArguments(listOf("--build-cache", REGISTER_TASK_NAME, "-i", "-s"))
                .forwardOutput()
                .build()
                .output

        assertTrue {
            buildResult.contains("Failed register $schema for $topic")
            buildResult.contains(
                "Unsupported subject name strategy! Allowed values: TopicNameStrategy, RecordNameStrategy, TopicRecordNameStrategy",
            )
        }
    }

    @Test
    fun `topic configs is empty exception thrown`() {
        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(testProject)

        val buildResult =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withEnvironment(System.getenv())
                .withArguments(listOf("--build-cache", REGISTER_TASK_NAME, "-i", "-s"))
                .forwardOutput()
                .build()
                .output

        assertTrue {
            buildResult.contains("Failed registerAllSchemas task!")
            buildResult.contains("Topic configs is empty!")
        }
    }

    @Test
    fun `file not found exception thrown when using schema from a protocol without specifying protocol in configs`() {
        val topic = randomString()
        val schema = "FirstExampleRecordFromProtocol"

        val exampleProtocolFile = exampleProtocol()

        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$topic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("$schema")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(testProject)
        testProjectDir.addAvroFiles(exampleProtocolFile)

        val buildResult =
            GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withEnvironment(System.getenv())
                .withArguments(listOf("--build-cache", REGISTER_TASK_NAME, "-i", "-s"))
                .forwardOutput()
                .build()
                .output

        assertTrue {
            buildResult.contains("Failed register $schema for $topic")
            buildResult.contains("File $schema not found!")
        }
    }

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
                )
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .waitingFor(Wait.forHealthcheck())
                .waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
                .dependsOn(kafkaContainer)

        init {
            Startables.deepStart(kafkaContainer, schemaRegistryContainer).join()
        }

        private val schemaRegistryUrl = "http://localhost:${schemaRegistryContainer.getMappedPort(8081)}"
    }
}
