@file:Suppress("MaxLineLength")

package io.github.rudikone.avroschemawizardplugin.compatibility

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
import java.io.File
import java.nio.file.Files

/**
 * Base for compatibility tests. Each test is fully isolated: it registers its own "before" schema
 * under a unique topic and checks the "after" schema against it, so different tests may use
 * different baseline schemas without needing separate classes or sharing subjects.
 */
@Suppress("LongParameterList")
abstract class BaseCompatibilityTest : BaseTaskTest() {
    protected fun record(vararg fields: String): Avro {
        val fieldsJson = fields.joinToString(",\n")
        return Avro(
            name = "Example.avsc",
            payLoad =
                """
                {
                    "type": "record",
                    "namespace": "ru.rudikov.example",
                    "name": "Example",
                    "fields": [
                        $fieldsJson
                    ]
                }
                """.trimIndent(),
        )
    }

    protected fun checkCompatible(
        before: Avro,
        after: Avro,
        compatibility: String,
        schemaName: String = "ru.rudikov.example.Example",
    ) {
        registerAndCheck(before, after, compatibility, schemaName, compatible = true)
    }

    protected fun checkNotCompatible(
        before: Avro,
        after: Avro,
        message: String,
        compatibility: String,
        schemaName: String = "ru.rudikov.example.Example",
    ) {
        registerAndCheck(before, after, compatibility, schemaName, compatible = false, message = message)
    }

    private fun registerAndCheck(
        before: Avro,
        after: Avro,
        compatibility: String,
        schemaName: String,
        compatible: Boolean,
        message: String = "",
    ) {
        val topic = randomString()
        val projectDir = newProject(topic)

        projectDir.addOrReplaceAvroFiles(before)
        buildProject(projectDir = projectDir, arguments = arrayOf(REGISTER_TASK_NAME))

        projectDir.addOrReplaceAvroFiles(after)
        val output =
            buildProject(
                projectDir = projectDir,
                arguments = arrayOf(COMPATIBILITY_CHECK_TASK_NAME, "--compatibility=$compatibility"),
            ).output

        val expected =
            "Schema $schemaName is ${if (compatible) "" else "not "}compatible with subject $topic-value. " +
                "Compatibility: $compatibility"

        assertTrue(
            output.contains(expected) && (compatible || output.contains(message)),
            "Expected ${if (compatible) "compatible" else "incompatible"} result.\nOutput:\n$output",
        )
    }

    private fun newProject(topic: String): File {
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
        val projectDir = Files.createTempDirectory("avro-wizard-compat").toFile()
        return ProjectDirGenerator.generate(project = testProject, projectDir = projectDir)
    }
}
