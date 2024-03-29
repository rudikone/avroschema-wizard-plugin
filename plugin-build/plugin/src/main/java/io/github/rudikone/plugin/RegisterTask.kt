package io.github.rudikone.plugin

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
    abstract val subjectToSchema: MapProperty<String, String>

    @TaskAction
    fun registerAllSchemas() {
        if (subjectToSchema.orNull.isNullOrEmpty()) {
            error("No schema has been announced!")
        }

        val registryClient = CachedSchemaRegistryClient(schemaRegistryUrl.get(), subjectToSchema.get().entries.size)

        registryClient.use { client ->
            subjectToSchema.get().forEach { (subject, schemaName) ->
                runCatching {
                    val searchPaths = searchAvroFilesPaths.get()
                    val avroFile = findAvroFileByName(searchPaths = searchPaths, schemaName = schemaName)
                    val schema = AvroSchema(Schema.Parser().parse(avroFile))

                    client.register(subject, schema)
                }.onSuccess {
                    logger.lifecycle("$schemaName: $it")
                }.onFailure {
                    logger.warn("Failed register $schemaName for $subject!", it)
                }
            }
        }
    }
}
