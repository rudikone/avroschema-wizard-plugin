package io.github.rudikone.avroschemawizardplugin

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.gradle.api.DefaultTask
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.options.Option

abstract class CompatibilityCheckTask : DefaultTask() {
    init {
        description = "Test compatibility of a schema with the latest schema under subject"
        group = "other"
    }

    @get:Input
    @get:Optional
    @get:Option(option = "subject", description = "Subject under which the schema is registered")
    abstract val subject: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "schema", description = "Schema to test for compatibility")
    abstract val schemaForCheck: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "compatibility", description = "Compatibility mode")
    abstract val compatibility: Property<String>

    @get:Input
    @get:Optional
    abstract val schemaRegistryUrl: Property<String>

    @get:Input
    abstract val subjectConfigs: MapProperty<String, SubjectConfig>

    @TaskAction
    fun checkCompatibility() {
        runCatching {
            if (subjectConfigs.orNull.isNullOrEmpty()) error("Subject configs must not be empty")

            val client = CachedSchemaRegistryClient(schemaRegistryUrl.get(), 1)

            if (subject.isPresent && schemaForCheck.isPresent) {
                client.use { testForConcreteSchema(it) }
            } else {
                client.use { testForAllSchemas(it) }
            }
        }.onFailure {
            logger.error("Compatibility test failed", it)
            throw TaskExecutionException(this, it)
        }
    }

    private fun testForConcreteSchema(client: SchemaRegistryClient) {
        val schemaName = schemaForCheck.get()
        val config =
            subjectConfigs
                .get()
                .values
                .find { it.schema.get() == schemaName }
                ?: error("No configuration found for schema $schemaName in avroWizardConfig")
        val schema = generateSchema(config)
        testCompatibility(client, schema, subject.get())
    }

    private fun testForAllSchemas(client: SchemaRegistryClient) {
        var allSuccess = true
        val fileCache = buildFileCache(subjectConfigs.get().values)

        subjectConfigs.get().forEach { (topic, config) ->
            runCatching {
                val nameStrategy = config.subjectNameStrategy.get().toSubjectNameStrategy()
                val schema = generateSchema(config, fileCache)
                val subject = nameStrategy.subjectName(topic, false, schema)
                testCompatibility(client, schema, subject)
            }.onFailure {
                allSuccess = false
                logger.error("Failed check compatibility ${config.schema.get()} for $topic!", it)
            }
        }

        if (!allSuccess) error("Compatibility test failed for some schemas")
    }

    private fun testCompatibility(
        client: SchemaRegistryClient,
        schema: AvroSchema,
        subject: String,
    ) {
        val schemaName = schema.rawSchema()?.fullName

        /*
         * Catching 40408 error code when subject does not have subject-level compatibility configured and
         * use subject-level compatibility.
         * See https://docs.confluent.io/cloud/current/sr/sr-rest-apis.html#get-the-compatibility-level-on-a-subject
         * */
        val originalCompatibility =
            runCatching {
                client.getCompatibility(subject)
            }.getOrElse {
                client.getCompatibility(null)
            }
        val newCompatibility = compatibility.orNull?.takeIf { it.isNotBlank() }

        newCompatibility?.let { client.updateCompatibility(subject, it) }

        val isCompatible = client.testCompatibility(subject, schema)
        logger.lifecycle(
            "Schema $schemaName is ${if (isCompatible) "compatible" else "not compatible"} with subject $subject. " +
                "Compatibility: ${newCompatibility ?: originalCompatibility}",
        )

        newCompatibility?.let { client.updateCompatibility(subject, originalCompatibility) }
    }
}
