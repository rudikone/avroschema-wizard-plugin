package io.github.rudikone.avroschemawizardplugin

import io.github.rudikone.avroschemawizardplugin.testutils.Avro
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator.addOrReplaceAvroFiles
import io.github.rudikone.avroschemawizardplugin.testutils.SimpleProject
import io.github.rudikone.avroschemawizardplugin.testutils.avroSchemaWizard
import io.github.rudikone.avroschemawizardplugin.testutils.buildProject
import io.github.rudikone.avroschemawizardplugin.testutils.exampleProtocol
import io.github.rudikone.avroschemawizardplugin.testutils.exampleSchema
import io.github.rudikone.avroschemawizardplugin.testutils.kotlinJvm
import io.github.rudikone.avroschemawizardplugin.testutils.randomString
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

@Suppress("MaxLineLength", "ktlint:standard:max-line-length")
class RegisterTaskTest : BaseTaskTest() {
    // see https://docs.gradle.org/current/userguide/compatibility.html
    @ParameterizedTest
    @CsvSource(
        "8.4, 1.8.0",
        "8.12, 1.8.0",
        "9.0.0, 2.2.0",
    )
    fun `schema is registered from avsc and avpr`(
        gradleVersion: String,
        kotlinVersion: String,
    ) {
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
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(exampleProtocolFile, exampleSchemaFile)

        val buildResult =
            buildProject(gradleVersion = gradleVersion, projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME)).output

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
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(exampleProtocolFile, exampleSchemaFile)

        val buildResult = buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME)).output

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
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Failed registerAllSchemas task!") == true
            buildResult.message?.contains("Topic configs is empty!") == true
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
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Failed registerAllSchemas task!") == true
            buildResult.message?.contains("Failed register $schema for $topic") == true
            buildResult.message?.contains(
                "Unsupported subject name strategy. Allowed: TopicNameStrategy, RecordNameStrategy, TopicRecordNameStrategy",
            ) ==
                true
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
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(exampleSchemaFile)

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Failed registerAllSchemas task!") == true
            buildResult.message?.contains("Failed register $randomSchema for $topic") == true
            buildResult.message?.contains("File $randomSchema not found!") == true
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
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(exampleProtocolFile)

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Failed registerAllSchemas task!") == true
            buildResult.message?.contains("Failed register $schema for $topic") == true
            buildResult.message?.contains("File $schema not found!") == true
        }
    }

    @Test
    fun `unknown host exception for each config thrown when registry host is unknown`() {
        val firstTopic = randomString()
        val secondTopic = randomString()
        val thirdTopic = randomString()

        val exampleProtocolFile = exampleProtocol()
        val exampleSchemaFile = exampleSchema()

        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("http://somehost:8080")
                configs {
                    topic("$firstTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        protocol.set("ExampleProtocol")
                        schema.set("FirstExampleRecordFromProtocol")
                    }
                    topic("$secondTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        protocol.set("ExampleProtocol")
                        schema.set("SecondExampleRecordFromProtocol")
                    }
                    topic("$thirdTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("Example")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(exampleProtocolFile, exampleSchemaFile)

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Failed registerAllSchemas task!") == true
            buildResult.message?.contains("Failed register FirstExampleRecordFromProtocol for $firstTopic") == true
            buildResult.message?.contains("Failed register SecondExampleRecordFromProtocol for $secondTopic") == true
            buildResult.message?.contains("Failed register Example for $thirdTopic") == true
        }
    }

    @Test
    @DisplayName("Default value must match the first type in a union in Avro schema")
    fun `invalid default exception thrown`() {
        val topic = randomString()
        val schema = "Example"

        val exampleSchemaFile =
            Avro(
                name = "$schema.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "$schema",
                        "fields": [
                            { "name": "Age", "type": [ "int", "null"], "default": null }
                        ]
                    }
                    """.trimIndent(),
            )

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
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(exampleSchemaFile)

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Failed registerAllSchemas task!") == true
            buildResult.message?.contains("Failed register $schema for $topic") == true
        }
    }

    companion object {
        @TempDir
        @JvmStatic
        private lateinit var tmp: File
    }
}
