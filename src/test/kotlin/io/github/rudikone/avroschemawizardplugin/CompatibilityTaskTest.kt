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
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

@Suppress("MaxLineLength", "LongMethod", "ktlint:standard:max-line-length")
@DisplayName("Tests for compatibility Check. Default compatibility used: BACKWARD")
class CompatibilityTaskTest : BaseTaskTest() {
    @ParameterizedTest
    @ValueSource(strings = ["1.9.0", "1.9.20", "2.0.0", "2.0.20", "2.1.0"])
    fun `schema from avsc by is compatible with the latest schema under subject`(
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

        // Adding a new optional field with a default value
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

        // Register schema before changes
        val registerTaskResult = buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        // Making changes to schema
        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        // Check compatibility
        val checkCompatibilityTaskResult =
            buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue {
            checkCompatibilityTaskResult.contains(
                "Schema ru.rudikov.example.Example is compatible with subject $topic-value. Compatibility: BACKWARD",
            )
        }
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

        // Changing the "Age" field type
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

        // Register schema before changes
        val registerTaskResult = buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        // Making changes to schema
        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        // Check compatibility
        val checkCompatibilityTaskResult =
            buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue {
            checkCompatibilityTaskResult.contains(
                "Schema ru.rudikov.example.Example is not compatible with subject $topic-value. Compatibility: BACKWARD",
            )
        }
    }

    @Test
    fun `schema from avpr by is compatible with the latest schema under subject`(
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

        // Adding a new optional field with a default value
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

        // Register schema before changes
        val registerTaskResult = buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        // Making changes to schema
        testProjectDir.addOrReplaceAvroFiles(protocolFileAfterChanges)

        // Check compatibility
        val checkCompatibilityTaskResult =
            buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue {
            checkCompatibilityTaskResult.contains(
                "Schema ru.rudikov.example.Example is compatible with subject $topic-value. Compatibility: BACKWARD",
            )
        }
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

        // Changing the "Age" field type
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

        // Register schema before changes
        val registerTaskResult = buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        // Making changes to schema
        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        // Check compatibility
        val checkCompatibilityTaskResult =
            buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME)).output

        assertTrue {
            checkCompatibilityTaskResult.contains(
                "Schema ru.rudikov.example.Example is not compatible with subject $topic-value. Compatibility: BACKWARD",
            )
        }
    }

    @Test
    fun `topic configs is empty exception thrown`(
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

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Compatibility test failure!") == true
            buildResult.message?.contains("Subject configs must not be empty") == true
        }
    }

    @Test
    fun `unsupported subject name strategy exception thrown`(
        @TempDir tmp: File,
    ) {
        val topic = randomString()

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

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Compatibility test failed") == true
            buildResult.message?.contains("Unsupported subject name strategy. Allowed: TopicNameStrategy, RecordNameStrategy, TopicRecordNameStrategy") == true
        }
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

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Compatibility test failure!") == true
            buildResult.message?.contains("Failed check compatibility $randomSchema for $topic") == true
            buildResult.message?.contains("File $randomSchema not found!") == true
        }
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

        val buildResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(projectDir = testProjectDir, arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME))
            }

        assertTrue {
            buildResult.message?.contains("Compatibility test failure!") == true
            buildResult.message?.contains("Failed check compatibility $schema for $topic") == true
            buildResult.message?.contains("File $schema not found!") == true
        }
    }

    @Test
    fun `unknown host exception for each config thrown when registry host is unknown`(
        @TempDir tmp: File,
    ) {
        val firstTopic = randomString()
        val secondTopic = randomString()

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

        // Config with correct schemaRegistryUrl
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
        testProjectDirForRegisterSchemas.addOrReplaceAvroFiles(protocolFileBeforeChanges)

        // Register schema before changes
        val registerTaskResult =
            buildProject(projectDir = testProjectDirForRegisterSchemas, arguments = arrayOf(REGISTER_TASK_NAME))
        assertEquals(SUCCESS, registerTaskResult.task(":$REGISTER_TASK_NAME")?.outcome)

        // Config with unknown schemaRegistryUrl
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

        // Check compatibility
        val checkCompatibilityTaskResult =
            assertThrows<UnexpectedBuildFailure> {
                buildProject(
                    projectDir = testProjectDirForCheckCompatibility,
                    arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME),
                )
            }

        assertTrue {
            checkCompatibilityTaskResult.message?.contains("Compatibility test failure!") == true
            checkCompatibilityTaskResult.message?.contains("Failed check compatibility FirstExampleRecordFromProtocol for $firstTopic") == true
            checkCompatibilityTaskResult.message?.contains("Failed check compatibility SecondExampleRecordFromProtocol for $secondTopic!") == true
        }
    }
}
