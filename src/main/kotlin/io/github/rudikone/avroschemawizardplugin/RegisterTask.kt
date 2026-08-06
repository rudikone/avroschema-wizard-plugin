package io.github.rudikone.avroschemawizardplugin

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.UntrackedTask

private const val SR_CLIENT_CACHE_CAPACITY = 1

@UntrackedTask(because = "Registers schemas in external Schema Registry")
abstract class RegisterTask : DefaultTask() {
    init {
        description = "Register all schemas"
        group = PLUGIN_TASK_GROUP
    }

    @get:Input
    @get:Optional
    abstract val schemaRegistryUrl: Property<String>

    // @Internal: таска @UntrackedTask, up-to-date отключён;
    // spec-и семантически не являются кэшируемым входом.
    @get:Internal
    abstract val subjectSpecs: ListProperty<SubjectSpec>

    @TaskAction
    fun registerAllSchemas() {
        logStart(logger)

        runCatching {
            val specs = subjectSpecs.get()
            if (specs.isEmpty()) error("Topic configs is empty!")

            val fileCache = buildFileCache(specs)
            var allSuccess = true

            CachedSchemaRegistryClient(schemaRegistryUrl.get(), SR_CLIENT_CACHE_CAPACITY).use { client ->
                specs.forEach { spec ->
                    var subject: String? = null
                    runCatching {
                        val nameStrategy = spec.subjectNameStrategy.toSubjectNameStrategy()
                        val schema = generateSchema(spec, fileCache)
                        subject = nameStrategy.subjectName(spec.topic, false, schema)
                        client.register(subject, schema)
                    }.onSuccess { id ->
                        logger.lifecycle(
                            "Registered ${spec.schema} with id: $id under subject $subject",
                        )
                    }.onFailure {
                        logger.warn("Failed register ${spec.schema} for ${spec.topic}!", it)
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
