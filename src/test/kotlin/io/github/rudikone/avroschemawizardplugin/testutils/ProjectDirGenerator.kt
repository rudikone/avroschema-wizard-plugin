package io.github.rudikone.avroschemawizardplugin.testutils

import java.io.File
import java.lang.System.lineSeparator

private const val SPACE = " "
private const val TAB_SPACE_COUNT = 4
private val SEPARATOR = lineSeparator()
private val TAB = SPACE.repeat(TAB_SPACE_COUNT)

object ProjectDirGenerator {
    fun generate(
        project: TestProject,
        projectDir: File,
    ): File {
        projectDir
            .resolve("build")
            .resolve("integrationTest")
            .resolve("projectDir")
            .also { it.mkdirs() }

        projectDir.resolve("build.gradle.kts").also { it.writeText(generateBuildText(project)) }
        projectDir.resolve("settings.gradle.kts").also { it.writeText(generateSettingGradle(project)) }

        return projectDir
    }

    fun File.addOrReplaceAvroFiles(vararg avroFiles: Avro): File {
        this
            .resolve("src")
            .resolve("resources")
            .also { it.mkdirs() }
            .also { resourcesDir ->
                avroFiles.forEach { avro ->
                    resourcesDir.resolve(avro.name).also { it.writeText(avro.payLoad) }
                }
            }

        return this
    }

    private fun generateBuildText(project: TestProject): String {
        val blocks =
            listOf(
                imports(project),
                plugins(project),
                allProjects(project),
                project.avroWizardConfig,
                dependencies(project),
                repositories(project),
            ).filter { it.isNotBlank() }

        val text = blocks.joinToString("$SEPARATOR$SEPARATOR")
        return text
    }

    private fun imports(project: TestProject): String =
        if (project.imports.isNotEmpty()) {
            project.imports.joinToString(SEPARATOR) { "import $it" }
        } else {
            ""
        }

    private fun plugins(project: TestProject): String =
        if (project.plugins.isNotEmpty()) {
            """plugins {
                |${project.plugins.joinToString(SEPARATOR) { TAB.plus(it.toString()) }}
                |}
            """.trimMargin()
        } else {
            ""
        }

    private fun allProjects(project: TestProject): String =
        """allprojects {
                |${TAB}version = "${project.version}"
                |${TAB}group = "${project.group}"
                |}
        """.trimMargin()

    private fun dependencies(project: TestProject): String =
        if (project.dependencies.isNotEmpty()) {
            """dependencies {
                |${project.dependencies.joinToString(SEPARATOR) { TAB.plus(it.toString()) }}
                |}
            """.trimMargin()
        } else {
            ""
        }

    private fun repositories(project: TestProject): String =
        if (project.repositories.isNotEmpty()) {
            """repositories {
                |${project.repositories.joinToString(SEPARATOR) { TAB.plus(it.toString()) }}
                |}
            """.trimMargin()
        } else {
            ""
        }

    /*
     * During test execution, the current version of the plugin is published to mavenLocal()
     * so that tests always use the latest build of the plugin.
     * See build.gradle.kts tasks.test
     */
    private fun generateSettingGradle(project: TestProject): String =
        """
            |rootProject.name = "${project.name}"
            |
            |pluginManagement {
            |    repositories {
            |        mavenLocal()
            |        mavenCentral()
            |    }
            |}
            |
        """.trimMargin()
}
