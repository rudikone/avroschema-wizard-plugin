package io.github.rudikone.plugin

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.confluent.kafka.serializers.subject.TopicNameStrategy
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy
import org.apache.avro.Schema
import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class RegisterTask : DefaultTask() {
    init {
        description = "Register all schemas"
        group = "other"
    }

    @get:Input
    @get:Optional
    abstract val schemaRegistryUrl: Property<String>

    @get:Input
    @get:Optional
    abstract val searchAvroFilesPaths: SetProperty<String>

    @get:Input
    abstract val topicToSchema: MapProperty<String, String>

    @get:Input
    @get:Optional
    abstract val subjectNameStrategy: Property<String>

    @TaskAction
    fun registerAllSchemas() {
        if (topicToSchema.orNull.isNullOrEmpty()) {
            error("No schema has been announced!")
        }

        val nameStrategyEnum =
            SubjectNameStrategies.from(subjectNameStrategy.get())
                ?: error("Unsupported subject name strategy! Allowed values: ${SubjectNameStrategies.values()}")

        val nameStrategy =
            when (nameStrategyEnum) {
                SubjectNameStrategies.TopicNameStrategy -> TopicNameStrategy()
                SubjectNameStrategies.RecordNameStrategy -> RecordNameStrategy()
                SubjectNameStrategies.TopicRecordNameStrategy -> TopicRecordNameStrategy()
            }

        val registryClient = CachedSchemaRegistryClient(schemaRegistryUrl.get(), topicToSchema.get().entries.size)

        registryClient.use { client ->
            topicToSchema.get().forEach { (topic, schemaName) ->
                runCatching {
                    val searchPaths = searchAvroFilesPaths.get()
                    val avroFile = findAvroFileByName(searchPaths = searchPaths, schemaName = schemaName)
                    val schema = AvroSchema(Schema.Parser().parse(avroFile))

                    val subject = nameStrategy.subjectName(topic, false, schema)

                    client.register(subject, schema)
                }.onSuccess {
                    logger.lifecycle("Registered $schemaName with id: $it for $topic")
                }.onFailure {
                    logger.warn("Failed register $schemaName for $topic!", it)
                }
            }
        }
    }
}
