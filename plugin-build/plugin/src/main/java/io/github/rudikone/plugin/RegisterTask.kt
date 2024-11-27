package io.github.rudikone.plugin

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.confluent.kafka.serializers.subject.TopicNameStrategy
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy
import org.apache.avro.Protocol
import org.apache.avro.Schema
import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
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
    abstract val subjectConfigs: MapProperty<String, SubjectConfig>

    @TaskAction
    fun registerAllSchemas() {
        if (subjectConfigs.orNull.isNullOrEmpty()) {
            error("Topic configs is empty!")
        }

        val registryClient = CachedSchemaRegistryClient(schemaRegistryUrl.get(), subjectConfigs.get().size)

        registryClient.use { client ->
            subjectConfigs.get().forEach { (topic, config) ->
                runCatching {
                    val nameStrategyEnum =
                        SubjectNameStrategies.from(config.subjectNameStrategy.get())
                            ?: error(
                                "Unsupported subject name strategy! " +
                                    "Allowed values: ${SubjectNameStrategies.entries.toTypedArray()}",
                            )

                    val nameStrategy =
                        when (nameStrategyEnum) {
                            SubjectNameStrategies.TopicNameStrategy -> TopicNameStrategy()
                            SubjectNameStrategies.RecordNameStrategy -> RecordNameStrategy()
                            SubjectNameStrategies.TopicRecordNameStrategy -> TopicRecordNameStrategy()
                        }

                    val fileName =
                        config.protocol.orNull
                            ?: config.schema.orNull
                            ?: error("No avro file is declared for a topic $topic!")

                    val searchPath =
                        config.searchAvroFilePath.orNull
                            ?: error("No path is declared to search for an avro file named $fileName!")

                    val avroFile = findAvroFileByName(path = searchPath, name = fileName)

                    if (avroFile.extension == AVSC) {
                        val schema = AvroSchema(Schema.Parser().parse(avroFile))
                        val subject = nameStrategy.subjectName(topic, false, schema)
                        client.register(subject, schema)
                    } else {
                        val protocol = Protocol.parse(avroFile)
                        val schema = AvroSchema(protocol.getType(config.schema.get()))
                        val subject = nameStrategy.subjectName(topic, false, schema)
                        client.register(subject, schema)
                    }
                }.onSuccess {
                    logger.lifecycle("Registered ${config.schema.get()} with id: $it for $topic")
                }.onFailure {
                    logger.warn("Failed register ${config.schema.get()} for $topic!", it)
                }
            }
        }
    }
}
