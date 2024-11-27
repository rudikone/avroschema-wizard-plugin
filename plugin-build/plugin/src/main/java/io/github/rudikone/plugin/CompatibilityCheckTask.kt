package io.github.rudikone.plugin

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
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
    abstract val subjectConfigs: MapProperty<String, SubjectConfig>

    @TaskAction
    fun checkCompatibility() {
        runCatching {
            if (subjectConfigs.orNull.isNullOrEmpty()) {
                error("Topic configs is empty!")
            }

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
        val registryClient = CachedSchemaRegistryClient(schemaRegistryUrl.get(), subjectConfigs.get().size)

        registryClient.use { client ->
            subjectConfigs.get().forEach { (topic, config) ->
                test(client = client, topic = topic, config = config)
            }
        }
    }

    private fun test(
        client: SchemaRegistryClient,
        topic: String,
        config: SubjectConfig,
    ) {
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
            config.protocol.orNull ?: config.schema.orNull ?: error("No avro file is declared for a topic $topic!")

        val searchPath =
            config.searchAvroFilePath.orNull ?: error("No path is declared to search for an avro file named $fileName!")

        val avroFile = findAvroFileByName(path = searchPath, name = fileName)

        if (avroFile.extension == AVSC) {
            val schema = AvroSchema(Schema.Parser().parse(avroFile))
            val subject = nameStrategy.subjectName(topic, false, schema)
            val isCompatible = client.testCompatibility(subject, schema)
            logCompatibleResult(schemaName = config.schema.get(), subject = subject, isCompatible = isCompatible)
        } else {
            val protocol = Protocol.parse(avroFile)
            val schema = AvroSchema(protocol.getType(config.schema.get()))
            val subject = nameStrategy.subjectName(topic, false, schema)
            val isCompatible = client.testCompatibility(subject, schema)
            logCompatibleResult(schemaName = config.schema.get(), subject = subject, isCompatible = isCompatible)
        }
    }

    private fun testForSubject(
        client: SchemaRegistryClient,
        subject: String,
        schemaName: String,
    ) {
        val config =
            subjectConfigs.get().values.find { it.schema.get() == schemaName }
                ?: error("No configuration declared in the avroWizardConfig block for the schema $schemaName!")

        val fileName =
            config.protocol.orNull
                ?: config.schema.orNull
                ?: error("No avro file is declared for a topic ${config.name}!")

        val searchPath =
            config.searchAvroFilePath.orNull
                ?: error("No path is declared to search for an avro file named $fileName!")

        val avroFile = findAvroFileByName(path = searchPath, name = fileName)

        if (avroFile.extension == AVSC) {
            val schema = AvroSchema(Schema.Parser().parse(avroFile))
            val isCompatible = client.testCompatibility(subject, schema)
            logCompatibleResult(schemaName = schemaName, subject = subject, isCompatible = isCompatible)
        } else {
            val protocol = Protocol.parse(avroFile)
            val schema = AvroSchema(protocol.getType(schemaName))
            val isCompatible = client.testCompatibility(subject, schema)
            logCompatibleResult(schemaName = schemaName, subject = subject, isCompatible = isCompatible)
        }
    }

    private fun logCompatibleResult(
        schemaName: String,
        subject: String,
        isCompatible: Boolean,
    ) {
        if (isCompatible) {
            val msg = "Schema $schemaName is compatible with the latest schema under subject $subject"
            logger.lifecycle(msg)
        } else {
            val msg = "Schema $schemaName is not compatible with the latest schema under subject $subject"
            logger.lifecycle(msg)
        }
    }
}
