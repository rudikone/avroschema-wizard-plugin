@file:Suppress("MaxLineLength", "LongMethod", "ktlint:standard:max-line-length")

package io.github.rudikone.avroschemawizardplugin.compatibility.full

import io.github.rudikone.avroschemawizardplugin.BaseTaskTest
import io.github.rudikone.avroschemawizardplugin.COMPATIBILITY_CHECK_TASK_NAME
import io.github.rudikone.avroschemawizardplugin.REGISTER_TASK_NAME
import io.github.rudikone.avroschemawizardplugin.testutils.Avro
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator.addOrReplaceAvroFiles
import io.github.rudikone.avroschemawizardplugin.testutils.SimpleProject
import io.github.rudikone.avroschemawizardplugin.testutils.buildProject
import io.github.rudikone.avroschemawizardplugin.testutils.randomString
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File

private const val COMPATIBILITY = "FULL"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("All allowed Avro schema evolutions are compatible under FULL compatibility. Upgrade first: Any order")
class FullCompatibilityTest : BaseTaskTest() {
    private val beforeChanges =
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
                        { "name": "LastName", "type": "string" },
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

    @Test
    fun `field with default value is added`() {
        val afterChanges =
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
                            { "name": "LastName", "type": "string" },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            { "name": "MiddleName", "type": "string", "default": "" }
                        ]
                    }
                    """.trimIndent(),
            )

        test(afterChanges)
    }

    @Test
    fun `record field with nested defaults is added`() {
        val afterChanges =
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
                            { "name": "LastName", "type": "string" },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            {
                                "name": "Address",
                                "type": {
                                    "type": "record",
                                    "name": "AddressRecord",
                                    "fields": [
                                        { "name": "City", "type": "string", "default": "Unknown" }
                                    ]
                                },
                                "default": {}
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        test(afterChanges)
    }

    @Test
    fun `array field with default is added`() {
        val afterChanges =
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
                            { "name": "LastName", "type": "string" },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            {
                                "name": "Hobbies",
                                "type": {
                                    "type": "array",
                                    "items": "string"
                                },
                                "default": []
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        test(afterChanges)
    }

    @Test
    fun `union field with default is added`() {
        val afterChanges =
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
                            { "name": "LastName", "type": "string" },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            { "name": "Score", "type": ["null", "int"], "default": null }
                        ]
                    }
                    """.trimIndent(),
            )

        test(afterChanges)
    }

    @Test
    fun `field with default is removed`() {
        val afterChanges =
            Avro(
                name = "Example.avsc",
                payLoad =
                    """
                    {
                        "type": "record",
                        "namespace": "ru.rudikov.example",
                        "name": "Example",
                        "fields": [
                            { "name": "Name", "type": ["null", "string"] },
                            { "name": "LastName", "type": "string" },
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

        test(afterChanges)
    }

    @BeforeAll
    fun setupProject() {
        topic = randomString()
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
        testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(beforeChanges)
        buildProject(projectDir = testProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))
    }

    private fun test(afterChanges: Avro) {
        // Making changes to schema
        testProjectDir.addOrReplaceAvroFiles(afterChanges)

        // Check compatibility
        val checkCompatibilityTaskResult =
            buildProject(
                projectDir = testProjectDir,
                arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME, "--compatibility=$COMPATIBILITY"),
            ).output

        assertTrue {
            checkCompatibilityTaskResult.contains(
                "Schema ru.rudikov.example.Example is compatible with subject $topic-value. Compatibility: $COMPATIBILITY",
            )
        }
    }

    companion object {
        @TempDir
        @JvmStatic
        private lateinit var tmp: File

        private lateinit var topic: String
        private lateinit var testProjectDir: File
    }
}
