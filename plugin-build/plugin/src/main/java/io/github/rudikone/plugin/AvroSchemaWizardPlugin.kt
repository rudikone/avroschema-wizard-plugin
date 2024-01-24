package io.github.rudikone.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

const val EXTENSION_NAME = "avroWizardConfig"
const val REGISTER_TASK_NAME = "registerAllSchemas"
const val COMPATIBILITY_CHECK_TASK_NAME = "checkCompatibility"

abstract class AvroSchemaWizardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, AvroWizardExtension::class.java, project)

        project.tasks.register(REGISTER_TASK_NAME, RegisterTask::class.java) {
            it.schemaRegistryUrl.set(extension.schemaRegistryUrl)
            it.searchAvroFilesPaths.set(extension.searchAvroFilesPaths)
            it.subjectToSchema.set(extension.subjectToSchema)
        }

        project.tasks.register(COMPATIBILITY_CHECK_TASK_NAME, CompatibilityCheckTask::class.java) {
            it.schemaRegistryUrl.set(extension.schemaRegistryUrl)
            it.searchAvroFilesPaths.set(extension.searchAvroFilesPaths)
            it.subjectToSchema.set(extension.subjectToSchema)
        }
    }
}
