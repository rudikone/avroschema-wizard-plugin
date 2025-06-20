package io.github.rudikone.avroschemawizardplugin

import org.gradle.api.Plugin
import org.gradle.api.Project

const val EXTENSION_NAME = "avroWizardConfig"
const val REGISTER_TASK_NAME = "registerAllSchemas"
const val COMPATIBILITY_CHECK_TASK_NAME = "checkCompatibility"

abstract class AvroSchemaWizardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, AvroWizardExtension::class.java, project)

        project.tasks.register(REGISTER_TASK_NAME, RegisterTask::class.java) {
            schemaRegistryUrl.set(extension.schemaRegistryUrl)
            subjectConfigs.set(
                project.provider {
                    extension.subjectConfigs.associateBy { config -> config.name }
                },
            )
        }

        project.tasks.register(COMPATIBILITY_CHECK_TASK_NAME, CompatibilityCheckTask::class.java) {
            schemaRegistryUrl.set(extension.schemaRegistryUrl)
            subjectConfigs.set(
                project.provider {
                    extension.subjectConfigs.associateBy { config -> config.name }
                },
            )
        }
    }
}
