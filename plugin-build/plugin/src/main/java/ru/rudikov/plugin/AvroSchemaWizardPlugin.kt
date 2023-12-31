package ru.rudikov.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

const val EXTENSION_NAME = "avroWizardConfig"
const val REGISTER_TASK_NAME = "registerAllSchemas"

abstract class AvroSchemaWizardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, AvroWizardExtension::class.java, project)

        project.tasks.register(REGISTER_TASK_NAME, RegisterTask::class.java) {
            it.schemaRegistryUrl.set(extension.schemaRegistryUrl)
            it.searchAvroFilesPaths.set(extension.searchAvroFilesPaths)
            it.subjectToSchema.set(extension.subjectToSchema)
        }
    }
}
