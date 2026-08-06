package io.github.rudikone.avroschemawizardplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register

const val EXTENSION_NAME = "avroWizardConfig"
const val REGISTER_TASK_NAME = "registerAllSchemas"
const val COMPATIBILITY_CHECK_TASK_NAME = "checkCompatibility"

const val PLUGIN_TASK_GROUP = "avro schema wizard"

abstract class AvroSchemaWizardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(EXTENSION_NAME, AvroWizardExtension::class.java)

        val specs: Provider<List<SubjectSpec>> =
            project.provider {
                extension.subjectConfigs.map { cfg ->
                    SubjectSpec(
                        topic = cfg.name,
                        searchAvroFilePath =
                            cfg.searchAvroFilePath
                                .orElse(extension.defaultSearchPath)
                                .get(),
                        protocol = cfg.protocol.orNull,
                        schema = cfg.schema.get(),
                        subjectNameStrategy = cfg.subjectNameStrategy.get(),
                    )
                }
            }

        project.tasks.register<RegisterTask>(REGISTER_TASK_NAME) {
            schemaRegistryUrl.set(extension.schemaRegistryUrl)
            subjectSpecs.set(specs)
        }

        project.tasks.register<CompatibilityCheckTask>(COMPATIBILITY_CHECK_TASK_NAME) {
            schemaRegistryUrl.set(extension.schemaRegistryUrl)
            subjectSpecs.set(specs)
        }
    }
}
