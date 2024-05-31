package io.github.rudikone.plugin

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
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
import org.gradle.api.tasks.options.Option

abstract class CompatibilityCheckTask : DefaultTask() {
    init {
        description = "Test compatibility of a schema with the latest schema under subject"
        group = "other"
    }

    @get:Input
    @get:Optional
    @get:Option(option = "subject", description = "A subject, under which lies the schema to be tested")
    abstract val subject: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "schema", description = "A schema for testing")
    abstract val schemaForCheck: Property<String>

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
    fun checkCompatibility() {
        runCatching {
            if (subject.isPresent && schemaForCheck.isPresent) {
                check()
            } else {
                checkForAllSchemas()
            }
        }.onFailure {
            logger.error("Compatibility test failure!", it)
        }
    }

    private fun check() {
        val client = CachedSchemaRegistryClient(schemaRegistryUrl.get(), 1)
        val schemaName = schemaForCheck.get()
        val subject = subject.get()

        client.use { testForSubject(client = it, subject = subject, schemaName = schemaName) }
    }

    private fun checkForAllSchemas() {
        if (topicToSchema.orNull.isNullOrEmpty()) {
            error("No schema has been announced!")
        }

        val registryClient = CachedSchemaRegistryClient(schemaRegistryUrl.get(), topicToSchema.get().entries.size)

        registryClient.use { client ->
            topicToSchema.get().forEach { (topic, schemaName) ->
                test(client = client, topic = topic, schemaName = schemaName)
            }
        }
    }

    private fun test(
        client: SchemaRegistryClient,
        topic: String,
        schemaName: String,
    ) {
        val avroFile = findAvroFileByName(searchPaths = searchAvroFilesPaths.get(), schemaName = schemaName)
        val schema = AvroSchema(Schema.Parser().parse(avroFile))

        val nameStrategyEnum =
            SubjectNameStrategies.from(subjectNameStrategy.get())
                ?: error("Unsupported subject name strategy! Allowed values: ${SubjectNameStrategies.values()}")

        val nameStrategy =
            when (nameStrategyEnum) {
                SubjectNameStrategies.TopicNameStrategy -> TopicNameStrategy()
                SubjectNameStrategies.RecordNameStrategy -> RecordNameStrategy()
                SubjectNameStrategies.TopicRecordNameStrategy -> TopicRecordNameStrategy()
            }

        val subject = nameStrategy.subjectName(topic, false, schema)

        val isCompatible = client.testCompatibility(subject, schema)

        if (isCompatible) {
            val msg = "Schema $schemaName is compatible with the latest schema under subject $subject"
            logger.lifecycle(msg)
        } else {
            val msg = "Schema $schemaName is not compatible with the latest schema under subject $subject"
            logger.lifecycle(msg)
        }
    }

    private fun testForSubject(
        client: SchemaRegistryClient,
        subject: String,
        schemaName: String,
    ) {
        val avroFile = findAvroFileByName(searchPaths = searchAvroFilesPaths.get(), schemaName = schemaName)
        val schema = AvroSchema(Schema.Parser().parse(avroFile))

        val isCompatible = client.testCompatibility(subject, schema)

        if (isCompatible) {
            val msg = "Schema $schemaName is compatible with the latest schema under subject $subject"
            logger.lifecycle(msg)
        } else {
            val msg = "Schema $schemaName is not compatible with the latest schema under subject $subject"
            logger.lifecycle(msg)
        }
    }
}
