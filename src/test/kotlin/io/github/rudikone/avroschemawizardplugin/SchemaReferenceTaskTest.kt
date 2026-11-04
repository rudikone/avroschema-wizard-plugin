package io.github.rudikone.avroschemawizardplugin

import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator
import io.github.rudikone.avroschemawizardplugin.testutils.ProjectDirGenerator.addOrReplaceAvroFiles
import io.github.rudikone.avroschemawizardplugin.testutils.SimpleProject
import io.github.rudikone.avroschemawizardplugin.testutils.buildProject
import io.github.rudikone.avroschemawizardplugin.testutils.buildProjectAndFail
import io.github.rudikone.avroschemawizardplugin.testutils.mainRecordSchema
import io.github.rudikone.avroschemawizardplugin.testutils.randomString
import io.github.rudikone.avroschemawizardplugin.testutils.sharedRecordSchema
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@Suppress("MaxLineLength", "ktlint:standard:max-line-length")
class SchemaReferenceTaskTest : BaseTaskTest() {
    private val sharedRecordFullName = "ru.rudikov.reference.SharedRecord"

    @Suppress("LongMethod")
    @Test
    fun `parent schema is registered with a reference to an already registered schema`() {
        val sharedTopic = randomString()
        val mainTopic = randomString()

        // Pre-register the referenced schema in a separate build so the parent only
        // references an already-registered subject (avoids a visibility race in the
        // test Schema Registry when a subject is registered and referenced in one run).
        val sharedConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$sharedTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("SharedRecord")
                    }
                }
            }
            """.trimIndent()
        val sharedProject = SimpleProject(avroWizardConfig = sharedConfig)
        val sharedProjectDir = ProjectDirGenerator.generate(project = sharedProject, projectDir = File(tmp, "shared"))
        sharedProjectDir.addOrReplaceAvroFiles(sharedRecordSchema())
        val sharedOutput =
            buildProject(
                projectDir = sharedProjectDir,
                arguments = arrayOf(REGISTER_TASK_NAME),
            ).output
        assertTrue(
            sharedOutput.contains(
                Regex("Registered SharedRecord with id: \\d+ under subject $sharedTopic-value"),
            ),
            "Expected registration of the referenced schema.\nOutput:\n$sharedOutput",
        )

        val mainConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$mainTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("MainRecord")
                        references {
                            reference {
                                name.set("$sharedRecordFullName")
                                subject.set("$sharedTopic-value")
                                version.set(1)
                            }
                        }
                    }
                }
            }
            """.trimIndent()
        val mainProject = SimpleProject(avroWizardConfig = mainConfig)
        val mainProjectDir = ProjectDirGenerator.generate(project = mainProject, projectDir = File(tmp, "main"))
        mainProjectDir.addOrReplaceAvroFiles(mainRecordSchema())

        val output =
            buildProject(
                projectDir = mainProjectDir,
                arguments = arrayOf(REGISTER_TASK_NAME),
            ).output
        assertTrue(
            output.contains(
                Regex("Registered MainRecord with id: \\d+ under subject $mainTopic-value"),
            ),
            "Expected registration of the parent schema with references.\nOutput:\n$output",
        )
    }

    @Test
    fun `build fails when reference subject does not exist`() {
        val mainTopic = randomString()
        val missingSubject = randomString()

        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$mainTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("MainRecord")
                        references {
                            reference {
                                name.set("$sharedRecordFullName")
                                subject.set("$missingSubject-value")
                                version.set(1)
                            }
                        }
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(mainRecordSchema())

        val output =
            buildProjectAndFail(
                projectDir = testProjectDir,
                arguments = arrayOf(REGISTER_TASK_NAME),
            ).output

        assertTrue(
            output.contains(
                "Cannot resolve reference '$sharedRecordFullName' for subject '$missingSubject-value' version 1",
            ),
            "Expected a clear unresolvable-reference error.\nOutput:\n$output",
        )
    }

    @Test
    fun `build fails when reference version does not exist`() {
        val sharedTopic = randomString()
        val mainTopic = randomString()
        val missingVersion = 999

        val avroWizardConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$sharedTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("SharedRecord")
                    }
                    topic("$mainTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("MainRecord")
                        references {
                            reference {
                                name.set("$sharedRecordFullName")
                                subject.set("$sharedTopic-value")
                                version.set($missingVersion)
                            }
                        }
                    }
                }
            }
            """.trimIndent()

        val testProject = SimpleProject(avroWizardConfig = avroWizardConfig)
        val testProjectDir = ProjectDirGenerator.generate(project = testProject, projectDir = tmp)
        testProjectDir.addOrReplaceAvroFiles(sharedRecordSchema(), mainRecordSchema())

        val output =
            buildProjectAndFail(
                projectDir = testProjectDir,
                arguments = arrayOf(REGISTER_TASK_NAME),
            ).output

        assertTrue(
            output.contains(
                "Cannot resolve reference '$sharedRecordFullName' for subject '$sharedTopic-value' version $missingVersion",
            ),
            "Expected a clear unresolvable-reference error.\nOutput:\n$output",
        )
    }

    @Test
    @Suppress("LongMethod")
    fun `registering a schema with references is compatible with configuration cache`() {
        val sharedTopic = randomString()
        val mainTopic = randomString()

        // Pre-register the referenced schema in a separate build so the parent only
        // references an already-registered subject (avoids a visibility race in the
        // test Schema Registry when a subject is registered and referenced in one run).
        val sharedConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$sharedTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("SharedRecord")
                    }
                }
            }
            """.trimIndent()
        val sharedProject = SimpleProject(avroWizardConfig = sharedConfig)
        val sharedProjectDir = ProjectDirGenerator.generate(project = sharedProject, projectDir = File(tmp, "shared"))
        sharedProjectDir.addOrReplaceAvroFiles(sharedRecordSchema())
        buildProject(projectDir = sharedProjectDir, arguments = arrayOf(REGISTER_TASK_NAME))

        val mainConfig =
            """
            avroWizardConfig {
                schemaRegistryUrl.set("$schemaRegistryUrl")
                configs {
                    topic("$mainTopic") {
                        searchAvroFilePath.set("${'$'}projectDir/src/resources")
                        schema.set("MainRecord")
                        references {
                            reference {
                                name.set("$sharedRecordFullName")
                                subject.set("$sharedTopic-value")
                                version.set(1)
                            }
                        }
                    }
                }
            }
            """.trimIndent()
        val mainProject = SimpleProject(avroWizardConfig = mainConfig)
        val mainProjectDir = ProjectDirGenerator.generate(project = mainProject, projectDir = File(tmp, "main"))
        mainProjectDir.addOrReplaceAvroFiles(mainRecordSchema())

        val firstRun =
            buildProject(
                projectDir = mainProjectDir,
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

        val secondRun =
            buildProject(
                projectDir = mainProjectDir,
                arguments = arrayOf(REGISTER_TASK_NAME, "--configuration-cache"),
            )
        assertTrue(
            secondRun.output.contains("Reusing configuration cache."),
            "Expected config cache to be reused on the second run.\nOutput:\n${secondRun.output}",
        )
    }

    companion object {
        @TempDir
        @JvmStatic
        private lateinit var tmp: File
    }
}
