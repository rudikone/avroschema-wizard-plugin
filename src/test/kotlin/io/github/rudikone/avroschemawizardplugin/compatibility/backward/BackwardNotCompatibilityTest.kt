@file:Suppress("MaxLineLength", "LongMethod", "ktlint:standard:max-line-length")

package io.github.rudikone.avroschemawizardplugin.compatibility.backward

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

private const val COMPATIBILITY = "BACKWARD"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Incompatible Avro schema changes are correctly rejected under BACKWARD compatibility. Upgrade first: Consumer")
class BackwardNotCompatibilityTest : BaseTaskTest() {
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
    fun `field type is changed`() {
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

        test(
            afterChanges = afterChanges,
            message = "reader type: STRING not compatible with writer type: INT",
        )
    }

    @Test
    fun `enum value is removed`() {
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

        test(
            afterChanges = afterChanges,
            message = "The new schema is missing enum symbols '[FEMALE]' at path '/fields/2/type/symbols' in the old schema', additionalInfo:'[FEMALE]",
        )
    }

    @Test
    fun `optional field becomes required`() {
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

        test(
            afterChanges = afterChanges,
            message = "The type (path '/') of a field in the new schema does not match with the old schema', additionalInfo:'reader type: STRING not compatible with writer type: NULL",
        )
    }

    @Test
    fun `field without default value is added`() {
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

        test(
            afterChanges = afterChanges,
            message = "The field 'LastName' at path '/fields/3' in the new schema has no default value and is missing in the old schema', additionalInfo:'LastName'",
        )
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

    private fun test(
        afterChanges: Avro,
        message: String,
    ) {
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
                "Schema ru.rudikov.example.Example is not compatible with subject $topic-value. Compatibility: $COMPATIBILITY",
            )
            checkCompatibilityTaskResult.contains(message)
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
