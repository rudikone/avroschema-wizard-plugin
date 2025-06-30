package io.github.rudikone.avroschemawizardplugin

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

abstract class RegisterTask : DefaultTask() {
    init {
        description = "Register all schemas"
        group = "other"
    }

    @get:Input
    @get:Optional
    abstract val schemaRegistryUrl: Property<String>

    @get:Input
    abstract val subjectConfigs: MapProperty<String, SubjectConfig>

    @TaskAction
    fun registerAllSchemas() {
        runCatching {
            if (subjectConfigs.orNull.isNullOrEmpty()) error("Topic configs is empty!")

            val registryClient = CachedSchemaRegistryClient(schemaRegistryUrl.get(), 1)
            var allSuccess = true

            registryClient.use { client ->
                subjectConfigs.get().forEach { (topic, config) ->
                    var subject: String? = null
                    runCatching {
                        val nameStrategy = config.subjectNameStrategy.get().toSubjectNameStrategy()
                        val schema = generateSchema(config)
                        subject = nameStrategy.subjectName(topic, false, schema)
                        client.register(subject, schema)
                    }.onSuccess {
                        logger.lifecycle("Registered ${config.schema.get()} with id: $it under subject $subject")
                    }.onFailure {
                        logger.warn("Failed register ${config.schema.get()} for $topic!", it)
                        allSuccess = false
                    }
                }
            }

            if (!allSuccess) error("Registration of some schema failed!")
        }.onFailure {
            logger.error("Failed $REGISTER_TASK_NAME task!", it)
            throw TaskExecutionException(this, it)
        }
    }
}
