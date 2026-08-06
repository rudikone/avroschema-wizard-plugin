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
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

@Suppress("MaxLineLength", "LongMethod", "ktlint:standard:max-line-length")
@DisplayName("Tests for compatibility Check. Default compatibility used: BACKWARD")
class CompatibilityTaskTest : BaseTaskTest() {
    // see https://docs.gradle.org/current/userguide/compatibility.html
    @ParameterizedTest
    @CsvSource(
        "8.4, 1.8.0",
        "8.12, 1.8.0",
        "9.0.0, 2.2.0",
    )
    fun `schema from avsc is compatible with the latest schema under subject`(
        gradleVersion: String,
        kotlinVersion: String,
        @TempDir tmp: File,
    ) {
        val topic = randomString()
        val schemaFileBeforeChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int" }
                        ]
                    }
                    """.trimIndent(),
            )
        val schemaFileAfterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int" },
                            { "name": "userName", "type": [ "null", "string" ], "default": null }
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
        testProjectDir.addOrReplaceAvroFiles(schemaFileBeforeChanges)

        val registerTaskResult =
            buildProject(gradleVersion = gradleVersion, projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        val output =
            buildProject(
                gradleVersion = gradleVersion,
                projectDir = testProjectDir,
                arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME),
            ).output

        assertTrue(
            output.contains(
                "Schema ru.rudikov.example.Example is compatible with subject $topic-value. Compatibility: BACKWARD",
            ),
            "Expected compatible result.\nOutput:\n$output",
        )
    }

    @Test
    fun `schema from avsc is not compatible with the latest schema under subject`(
        @TempDir tmp: File,
    ) {
        val topic = randomString()
        val schemaFileBeforeChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int" }
                        ]
                    }
                    """.trimIndent(),
            )
        val schemaFileAfterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "boolean" }
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
                        schema.set("Example")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(schemaFileBeforeChanges)

        val registerTaskResult = buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        val output = buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue(
            output.contains(
                "Schema ru.rudikov.example.Example is not compatible with subject $topic-value. Compatibility: BACKWARD",
            ),
            "Expected incompatible result.\nOutput:\n$output",
        )
    }

    @Test
    fun `schema from avpr is compatible with the latest schema under subject`(
        @TempDir tmp: File,
    ) {
        val topic = randomString()
        val protocolFileBeforeChanges =
            Avro(
                name = "ExampleProtocol.avpr",
                payLoad =
                    """
                    {
                        "protocol" : "ExampleProtocol",
                        "namespace" : "ru.rudikov.example",
                        "types" : [ {
                            "type" : "record",
                            "name" : "Example",
                            "fields" : [ {
                                "name" : "age",
                                "type" : "int"
                            } ]
                        } ],
                        "messages" : { }
                    }
                    """.trimIndent(),
            )
        val protocolFileAfterChanges =
            Avro(
                name = "ExampleProtocol.avpr",
                payLoad =
                    """
                    {
                        "protocol" : "ExampleProtocol",
                        "namespace" : "ru.rudikov.example",
                        "types" : [ {
                            "type" : "record",
                            "name" : "Example",
                            "fields" : [ {
                                "name" : "age",
                                "type" : "int"
                            }, {
                                "name" : "userName",
                                "type" : [ "null", "string" ],
                                "default" : null
                            } ]
                        } ],
                        "messages" : { }
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
                        protocol.set("ExampleProtocol")
                        schema.set("Example")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(protocolFileBeforeChanges)

        val registerTaskResult = buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        testProjectDir.addOrReplaceAvroFiles(protocolFileAfterChanges)

        val output = buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue(
            output.contains(
                "Schema ru.rudikov.example.Example is compatible with subject $topic-value. Compatibility: BACKWARD",
            ),
            "Expected compatible result.\nOutput:\n$output",
        )
    }

    @Test
    fun `schema from avpr is not compatible with the latest schema under subject`(
        @TempDir tmp: File,
    ) {
        val topic = randomString()
        val schemaFileBeforeChanges =
            Avro(
                name = "ExampleProtocol.avpr",
                payLoad =
                    """
                    {
                        "protocol" : "ExampleProtocol",
                        "namespace" : "ru.rudikov.example",
                        "types" : [ {
                            "type" : "record",
                            "name" : "Example",
                            "fields" : [ {
                                "name" : "age",
                                "type" : "int"
                            } ]
                        } ],
                        "messages" : { }
                    }
                    """.trimIndent(),
            )
        val schemaFileAfterChanges =
            Avro(
                name = "ExampleProtocol.avpr",
                payLoad =
                    """
                    {
                        "protocol" : "ExampleProtocol",
                        "namespace" : "ru.rudikov.example",
                        "types" : [ {
                            "type" : "record",
                            "name" : "Example",
                            "fields" : [ {
                                "name" : "age",
                                "type" : "boolean"
                            } ]
                        } ],
                        "messages" : { }
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
                        protocol.set("ExampleProtocol")
                        schema.set("Example")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(schemaFileBeforeChanges)

        val registerTaskResult = buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        val output = buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue(
            output.contains(
                "Schema ru.rudikov.example.Example is not compatible with subject $topic-value. Compatibility: BACKWARD",
            ),
            "Expected incompatible result.\nOutput:\n$output",
        )
    }

    @Test
    fun `subject configs empty exception thrown`(
        @TempDir tmp: File,
    ) {
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

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue(
            output.contains("Compatibility test failed") &&
                output.contains("Subject configs must not be empty"),
            "Expected empty subject configs failure.\nOutput:\n$output",
        )
    }

    @Test
    fun `unsupported subject name strategy exception thrown`(
        @TempDir tmp: File,
    ) {
        val topic = randomString()
        val schemaFile =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int" }
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
                        schema.set("Example")
                        subjectNameStrategy.set("Unknown")
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(schemaFile)

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue(
            output.contains("Compatibility test failed") &&
                output.contains(
                    "Unsupported subject name strategy. Allowed: TopicNameStrategy, RecordNameStrategy, TopicRecordNameStrategy",
                ),
            "Expected unsupported-strategy failure.\nOutput:\n$output",
        )
    }

    @Test
    fun `file not found exception thrown when avro file in resources is missing`(
        @TempDir tmp: File,
    ) {
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

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue(
            output.contains("Compatibility test failed") &&
                output.contains("Failed check compatibility $randomSchema for $topic") &&
                output.contains("File $randomSchema not found in configured search path(s)!"),
            "Expected file-not-found failure.\nOutput:\n$output",
        )
    }

    @Test
    fun `file not found exception thrown when using schema from a protocol without specifying protocol in configs`(
        @TempDir tmp: File,
    ) {
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

        val output = buildProjectAndFail(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue(
            output.contains("Compatibility test failed") &&
                output.contains("Failed check compatibility $schema for $topic") &&
                output.contains("File $schema not found in configured search path(s)!"),
            "Expected file-not-found-in-protocol failure.\nOutput:\n$output",
        )
    }

    @Test
    fun `unknown host exception for each config thrown when registry host is unknown`(
        @TempDir tmp: File,
    ) {
        val firstTopic = randomString()
        val secondTopic = randomString()
        val protocolFile =
            Avro(
                name = "ExampleProtocol.avpr",
                payLoad =
                    """
                    {
                        "protocol" : "ExampleProtocol",
                        "namespace" : "ru.rudikov.example",
                        "types" : [ {
                            "type" : "record",
                            "name" : "FirstExampleRecordFromProtocol",
                            "fields" : [ {
                                "name" : "age",
                                "type" : "int"
                            } ]
                        }, {
                            "type" : "record",
                            "name" : "SecondExampleRecordFromProtocol",
                            "fields" : [ {
                                "name" : "color",
                                "type" : "string"
                            } ]
                        } ],
                        "messages" : { }
                    }
                    """.trimIndent(),
            )

        val avroWizardConfigForRegisterSchemas =
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
                        protocol.set("ExampleProtocol")
                        schema.set("SecondExampleRecordFromProtocol")
                    }
                }
            }
            """.trimIndent()

        val testProjectForRegisterSchemas = SimpleProject(avroWizardConfig = avroWizardConfigForRegisterSchemas)
        val testProjectDirForRegisterSchemas =
            ProjectDirGenerator.generate(project = testProjectForRegisterSchemas, projectDir = tmp)
        testProjectDirForRegisterSchemas.addOrReplaceAvroFiles(protocolFile)

        val registerTaskResult =
            buildProject(projectDir = testProjectDirForRegisterSchemas, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        val avroWizardConfigForCheckCompatibility =
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
                }
            }
            """.trimIndent()

        val testProjectForCheckCompatibility = SimpleProject(avroWizardConfig = avroWizardConfigForCheckCompatibility)
        val testProjectDirForCheckCompatibility =
            ProjectDirGenerator.generate(project = testProjectForCheckCompatibility, projectDir = tmp)
        testProjectDirForCheckCompatibility.addOrReplaceAvroFiles(protocolFile)

        val output =
            buildProjectAndFail(
                projectDir = testProjectDirForCheckCompatibility,
                arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME),
            ).output

        assertTrue(
            output.contains("Compatibility test failed") &&
                output.contains("Failed check compatibility FirstExampleRecordFromProtocol for $firstTopic") &&
                output.contains("Failed check compatibility SecondExampleRecordFromProtocol for $secondTopic"),
            "Expected unknown-host failure for every config.\nOutput:\n$output",
        )
    }
}
