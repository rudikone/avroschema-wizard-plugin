@file:Suppress("LongParameterList")

package io.github.rudikone.avroschemawizardplugin.testutils

abstract class TestProject {
    abstract val name: String
    abstract val imports: MutableList<String>
    abstract val plugins: MutableList<Plugin>
    abstract val group: String
    abstract val version: String
    abstract val avroWizardConfig: String
    abstract val dependencies: MutableList<Dependency>
    abstract val repositories: MutableList<Repository>
}

class SimpleProject(
    override val name: String = "simple-project",
    override val imports: MutableList<String> = mutableListOf("io.github.rudikone.avroschemawizardplugin.topic"),
    override val plugins: MutableList<Plugin> = mutableListOf(kotlinJvm(), avroSchemaWizard()),
    override val group: String = "io.github.rudikone",
    override val version: String = "0.1.0",
    override val avroWizardConfig: String,
    override val dependencies: MutableList<Dependency> = mutableListOf(),
    override val repositories: MutableList<Repository> = mutableListOf(),
) : TestProject()
