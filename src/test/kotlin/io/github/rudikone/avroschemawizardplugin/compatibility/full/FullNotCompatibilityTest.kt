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
@DisplayName("Incompatible Avro schema changes are correctly rejected under FULL compatibility. Upgrade first: Any order")
class FullNotCompatibilityTest : BaseTaskTest() {
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
    fun `field type int extended to long`() {
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
                            { "name": "Age", "type": "long", "default": 0 },
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

        test(
            afterChanges = afterChanges,
            message = "description:'The type (path '/fields/0/type') of a field in the old schema does not match with the new schema', additionalInfo:'reader type: INT not compatible with writer type: LONG'",
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
                            { "name": "LastName", "type": "string" },
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
            message = "description:'The new schema is missing enum symbols '[FEMALE]' at path '/fields/3/type/symbols' in the old schema', additionalInfo:'[FEMALE]'",
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

        test(
            afterChanges = afterChanges,
            message = "description:'The type (path '/') of a field in the new schema does not match with the old schema', additionalInfo:'reader type: STRING not compatible with writer type: NULL'",
        )
    }

    @Test
    fun `field without default is removed`() {
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
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        test(
            afterChanges = afterChanges,
            message = "description:'The field 'LastName' at path '/fields/2' in the old schema has no default value and is missing in the new schema', additionalInfo:'LastName'",
        )
    }

    @Test
    fun `required field becomes optional`() {
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
                            { "name": "Age", "type": ["int", "null"], "default": 0 },
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

        test(
            afterChanges = afterChanges,
            message = "description:'The type (path '/') of a field in the old schema does not match with the new schema', additionalInfo:'reader type: INT not compatible with writer type: NULL'",
        )
    }

    @Test
    fun `new union type is added`() {
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
                            { "name": "Name", "type": ["null", "string", "int"] },
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

        test(
            afterChanges = afterChanges,
            message = "description:'The old schema is missing a type inside a union field at path '/fields/1/type/2' in the new schema', additionalInfo:'reader union lacking writer type: INT'",
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
                            { "name": "LastName", "type": "string" },
                            {
                                "name": "Gender",
                                "type": {
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE"]
                                 }
                            },
                            { "name": "MiddleName", "type": "string" }
                        ]
                    }
                    """.trimIndent(),
            )

        test(
            afterChanges = afterChanges,
            message = "description:'The field 'MiddleName' at path '/fields/4' in the new schema has no default value and is missing in the old schema', additionalInfo:'MiddleName'",
        )
    }

    @Test
    fun `enum value is added`() {
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
                                    "type": "enum", "name": "Gender", "symbols": ["MALE", "FEMALE", "OTHER"]
                                 }
                            }
                        ]
                    }
                    """.trimIndent(),
            )

        test(
            afterChanges = afterChanges,
            message = "description:'The old schema is missing enum symbols '[OTHER]' at path '/fields/3/type/symbols' in the new schema', additionalInfo:'[OTHER]'",
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
