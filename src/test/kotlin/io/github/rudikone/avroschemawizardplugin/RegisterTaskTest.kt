package io.github.rudikone.avroschemawizardplugin

import io.github.rudikone.avroschemawizardplugin.testutils.Avro
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator.addOrReplaceAvroFiles
import io.github.rudikone.avroschemawizardplugin.testutils.SimpleProject
import io.github.rudikone.avroschemawizardplugin.testutils.avroSchemaWizard
import io.github.rudikone.avroschemawizardplugin.testutils.buildProject
import io.github.rudikone.avroschemawizardplugin.testutils.buildProjectAndFail
import io.github.rudikone.avroschemawizardplugin.testutils.exampleProtocol
import io.github.rudikone.avroschemawizardplugin.testutils.exampleSchema
import io.github.rudikone.avroschemawizardplugin.testutils.kotlinJvm
import io.github.rudikone.avroschemawizardplugin.testutils.randomString
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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

        val output =
            buildProject(
                gradleVersion = gradleVersion,
                projectDir = testProjectDir,
                arguments = arrayOf(REGISTER_TASK_NAME),
            ).output

        assertTrue(
            output.contains(
                Regex("Registered FirstExampleRecordFromProtocol with id: \\d+ under subject $firstTopic-value"),
            ),
            "Expected registration log for FirstExampleRecordFromProtocol.\nOutput:\n$output",
        )
        assertTrue(
            output.contains(Regex("Registered Example with id: \\d+ under subject $secondTopic-value")),
            "Expected registration log for Example.\nOutput:\n$output",
        )
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

        val output = buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME)).output

        assertTrue(
            output.contains(
                Regex("Registered FirstExampleRecordFromProtocol with id: \\d+ under subject $firstTopic-value"),
            ),
            "Missing TopicNameStrategy registration.\nOutput:\n$output",
        )
        assertTrue(
            output.contains(
                Regex(
                    "Registered SecondExampleRecordFromProtocol with id: \\d+ under subject ru.rudikov.example.SecondExampleRecordFromProtocol",
                ),
            ),
            "Missing RecordNameStrategy registration.\nOutput:\n$output",
        )
        assertTrue(
            output.contains(
                Regex("Registered Example with id: \\d+ under subject $thirdTopic-ru.rudikov.example.ExampleRecordFromSchema"),
            ),
            "Missing TopicRecordNameStrategy registration.\nOutput:\n$output",
        )
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

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME)).output

        assertTrue(
            output.contains("Failed registerAllSchemas task!") &&
                output.contains("Topic configs is empty!"),
            "Expected empty-config failure.\nOutput:\n$output",
        )
    }

    @Test
    fun `unsupported subject name strategy exception thrown`() {
        val topic = randomString()
        val exampleSchemaFile = exampleSchema()
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
        testProjectDir.addOrReplaceAvroFiles(exampleSchemaFile)

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME)).output

        assertTrue(
            output.contains("Failed registerAllSchemas task!") &&
                output.contains("Failed register $schema for $topic") &&
                output.contains(
                    "Unsupported subject name strategy. Allowed: TopicNameStrategy, RecordNameStrategy, TopicRecordNameStrategy",
                ),
            "Expected unsupported-strategy failure.\nOutput:\n$output",
        )
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

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME)).output

        assertTrue(
            output.contains("Failed registerAllSchemas task!") &&
                output.contains("Failed register $randomSchema for $topic") &&
                output.contains("File $randomSchema not found in configured search path(s)!"),
            "Expected file-not-found failure.\nOutput:\n$output",
        )
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

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME)).output

        assertTrue(
            output.contains("Failed registerAllSchemas task!") &&
                output.contains("Failed register $schema for $topic") &&
                output.contains("File $schema not found in configured search path(s)!"),
            "Expected file-not-found-in-protocol failure.\nOutput:\n$output",
        )
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

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME)).output

        assertTrue(
            output.contains("Failed registerAllSchemas task!") &&
                output.contains("Failed register FirstExampleRecordFromProtocol for $firstTopic") &&
                output.contains("Failed register SecondExampleRecordFromProtocol for $secondTopic") &&
                output.contains("Failed register Example for $thirdTopic"),
            "Expected unknown-host failure for every config.\nOutput:\n$output",
        )
    }

    @Test
    fun `registerAllSchemas is compatible with configuration cache`() {
        val topic = randomString()
        val exampleSchemaFile = exampleSchema()

        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$topic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("Example")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(exampleSchemaFile)

        // First run: cache is stored. A real serialization problem would fail the build
        // (Gradle default: configuration-cache-problems=fail).
        val firstRun =
            buildProject(
                projectDir = testProjectDir,
                arguments = arrayOf(REGISTER_TASK_NAME, "--configuration-cache"),
            )
        assertTrue(
            firstRun.output.contains("Configuration cache entry stored."),
            "Expected config cache to be stored on the first run.\nOutput:\n${firstRun.output}",
        )
        assertTrue(
            firstRun.output.contains("0 problems were found storing the configuration cache."),
            "Expected zero configuration cache problems on the first run.\nOutput:\n${firstRun.output}",
        )

        // Second run: cache is reused.
        val secondRun =
            buildProject(
                projectDir = testProjectDir,
                arguments = arrayOf(REGISTER_TASK_NAME, "--configuration-cache"),
            )
        assertTrue(
            secondRun.output.contains("Reusing configuration cache."),
            "Expected config cache to be reused on the second run.\nOutput:\n${secondRun.output}",
        )
    }

    @Test
    @DisplayName("Default value must match the first type in a union in Avro schema")
    @Disabled(
        """
        Disabled after migration to 8.1.1 version for
        io.confluent:kafka-schema-registry-client and io.confluent:kafka-schema-serializer.
        See https://github.com/confluentinc/schema-registry/issues/4112
    """,
    )
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

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME)).output

        assertTrue(
            output.contains("Failed registerAllSchemas task!") &&
                output.contains("Failed register $schema for $topic"),
            "Expected invalid-default failure.\nOutput:\n$output",
        )
    }

    companion object {
        @TempDir
        @JvmStatic
        private lateinit var tmp: File
    }
}
