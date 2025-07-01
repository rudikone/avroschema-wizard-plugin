package io.github.rudikone.avroschemawizardplugin.compatibility.backward

import io.github.rudikone.avroschemawizardplugin.BaseTaskTest
import io.github.rudikone.avroschemawizardplugin.COMPATIBILITY_CHECK_TASK_NAME
import io.github.rudikone.avroschemawizardplugin.REGISTER_TASK_NAME
import io.github.rudikone.avroschemawizardplugin.testutils.Avro
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator.addOrReplaceAvroFiles
import io.github.rudikone.avroschemawizardplugin.testutils.SimpleProject
import io.github.rudikone.avroschemawizardplugin.testutils.TestProject
import io.github.rudikone.avroschemawizardplugin.testutils.buildProject
import io.github.rudikone.avroschemawizardplugin.testutils.randomString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File

private const val COMPATIBILITY = "BACKWARD"

@Suppress("MaxLineLength", "LongMethod", "ktlint:standard:max-line-length")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Incompatible Avro schema changes are correctly rejected under BACKWARD compatibility. Upgrade first: Consumer")
class BackwardNotCompatibilityTest : BaseTaskTest() {
    private lateinit var topic: String
    private lateinit var schemaFileBeforeChanges: Avro
    private lateinit var avroWizardConfig: String
    private lateinit var testProject: TestProject
    private lateinit var testProjectDir: File

    @BeforeAll
    fun setupProject() {
        topic = randomString()

        schemaFileBeforeChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Age", "type": "int", "default": 0 },
                            { "name": "Name", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        avroWizardConfig =
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

        testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(schemaFileBeforeChanges)
        buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
    }

    @Test
    fun `field type is changed`() {
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
                            { "name": "Age", "type": "string", "default": "0" },
                            { "name": "Name", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        // Making changes to schema
        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        // Check compatibility
        val checkCompatibilityTaskResult =
            buildProject(
                projectDir = testProjectDir,
                arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME, "--compatibility=$COMPATIBILITY"),
            ).output

        Assertions.assertTrue {
            checkCompatibilityTaskResult.contains(
                "Schema ru.rudikov.example.Example is not compatible with subject $topic-value. Compatibility: $COMPATIBILITY",
            )
        }
    }

    @Test
    fun `enum value is removed`() {
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
                            { "name": "Age", "type": "string", "default": "0" },
                            { "name": "Name", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE"]
                                 }
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        // Making changes to schema
        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        // Check compatibility
        val checkCompatibilityTaskResult =
            buildProject(
                projectDir = testProjectDir,
                arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME, "--compatibility=$COMPATIBILITY"),
            ).output

        Assertions.assertTrue {
            checkCompatibilityTaskResult.contains(
                "Schema ru.rudikov.example.Example is not compatible with subject $topic-value. Compatibility: $COMPATIBILITY",
            )
        }
    }

    @Test
    fun `optional field becomes required`() {
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
                            { "name": "Age", "type": "int", "default": 0 },
                            { "name": "Name", "type": "string" },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        // Making changes to schema
        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        // Check compatibility
        val checkCompatibilityTaskResult =
            buildProject(
                projectDir = testProjectDir,
                arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME, "--compatibility=$COMPATIBILITY"),
            ).output

        Assertions.assertTrue {
            checkCompatibilityTaskResult.contains(
                "Schema ru.rudikov.example.Example is not compatible with subject $topic-value. Compatibility: $COMPATIBILITY",
            )
        }
    }

    @Test
    fun `field without default value is added`() {
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
                            { "name": "Age", "type": "int", "default": 0 },
                            { "name": "Name", "type": ["null", "string"] },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            { "name": "LastName", "type": "string" }
                        ]
                    }
                    """.trimIndent(),
            )

        // Making changes to schema
        testProjectDir.addOrReplaceAvroFiles(schemaFileAfterChanges)

        // Check compatibility
        val checkCompatibilityTaskResult =
            buildProject(
                projectDir = testProjectDir,
                arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME, "--compatibility=$COMPATIBILITY"),
            ).output

        Assertions.assertTrue {
            checkCompatibilityTaskResult.contains(
                "Schema ru.rudikov.example.Example is not compatible with subject $topic-value. Compatibility: $COMPATIBILITY",
            )
        }
    }

    companion object {
        @TempDir
        @JvmStatic
        private lateinit var tmp: File
    }
}
