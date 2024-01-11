package ru.rudikov.plugin

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import org.apache.avro.Schema
import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class CompatibilityCheckTask : DefaultTask() {
    init {
        description = "Test compatibility of a schema with the latest schema under subject"
        group = "other"
    }

    @get:Input
    @get:Optional
    abstract val schemaRegistryUrl: Property<String>

    @get:Input
    @get:Optional
    abstract val searchAvroFilesPaths: SetProperty<String>

    @get:Input
    abstract val subjectToSchema: MapProperty<String, String>

    @TaskAction
    fun checkCompatibility() {
        if (subjectToSchema.orNull.isNullOrEmpty()) {
            error("No schema has been announced!")
        }

        val client = CachedSchemaRegistryClient(schemaRegistryUrl.get(), subjectToSchema.get().entries.size)

        runCatching {
            subjectToSchema.get().forEach { (subject, schemaName) ->
                searchAvroFilesPaths.get().forEach { searchPath ->
                    val avroFile = findAvroFileByName(searchPath = searchPath, schemaName = schemaName)
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
        }.onFailure {
            logger.error("Compatibility test failure!", it)
        }
    }
}
