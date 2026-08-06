package io.github.rudikone.avroschemawizardplugin

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option

private const val SR_CLIENT_CACHE_CAPACITY = 1

@UntrackedTask(because = "Checks schemas in external Schema Registry")
abstract class CompatibilityCheckTask : DefaultTask() {
    init {
        description = "Test compatibility of a schema with the latest schema under subject"
        group = PLUGIN_TASK_GROUP
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

    @get:Internal
    abstract val subjectSpecs: ListProperty<SubjectSpec>

    @TaskAction
    fun checkCompatibility() {
        logStart(logger)

        runCatching {
            val specs = subjectSpecs.get()
            if (specs.isEmpty()) error("Subject configs must not be empty")

            CachedSchemaRegistryClient(schemaRegistryUrl.get(), SR_CLIENT_CACHE_CAPACITY).use { client ->
                if (subject.isPresent && schemaForCheck.isPresent) {
                    testForConcreteSchema(client, specs)
                } else {
                    testForAllSchemas(client, specs)
                }
            }
        }.onFailure {
            logger.error("Compatibility test failed", it)
            throw TaskExecutionException(this, it)
        }
    }

    private fun testForConcreteSchema(
        client: SchemaRegistryClient,
        specs: List<SubjectSpec>,
    ) {
        val schemaName = schemaForCheck.get()
        val spec =
            specs.find { it.schema == schemaName }
                ?: error("No configuration found for schema $schemaName in $EXTENSION_NAME")

        val fileCache = buildFileCache(specs)
        val newSchema = generateSchema(spec, fileCache)
        testCompatibility(client, newSchema, subject.get())
    }

    private fun testForAllSchemas(
        client: SchemaRegistryClient,
        specs: List<SubjectSpec>,
    ) {
        var allSuccess = true
        val fileCache = buildFileCache(specs)

        specs.forEach { spec ->
            runCatching {
                val nameStrategy = spec.subjectNameStrategy.toSubjectNameStrategy()
                val newSchema = generateSchema(spec, fileCache)
                val computedSubject = nameStrategy.subjectName(spec.topic, false, newSchema)
                testCompatibility(client, newSchema, computedSubject)
            }.onFailure {
                allSuccess = false
                logger.error("Failed check compatibility ${spec.schema} for ${spec.topic}!", it)
            }
        }

        if (!allSuccess) error("Compatibility test failed for some schemas")
    }

    private fun testCompatibility(
        client: SchemaRegistryClient,
        newSchema: AvroSchema,
        subject: String,
    ) {
        val schemaName = newSchema.rawSchema()?.fullName

        /*
         * Catching 40408 error code when subject does not have subject-level compatibility configured
         * and use global compatibility.
         * https://docs.confluent.io/cloud/current/sr/sr-rest-apis.html#get-the-compatibility-level-on-a-subject
         */
        val originalCompatibility =
            runCatching { client.getCompatibility(subject) }
                .getOrElse { client.getCompatibility(null) }

        val newCompatibility = compatibility.orNull?.takeIf { it.isNotBlank() }
        newCompatibility?.let { client.updateCompatibility(subject, it) }

        try {
            val incompatibilities = client.testCompatibilityVerbose(subject, newSchema)
            if (incompatibilities.isNotEmpty()) {
                val message =
                    buildString {
                        appendLine(
                            "Schema $schemaName is not compatible with subject $subject. " +
                                "Compatibility: ${newCompatibility ?: originalCompatibility}",
                        )
                        incompatibilities.forEach { inc ->
                            append(" -> ")
                            appendLine(inc)
                        }
                    }
                logger.lifecycle(message)
            } else {
                logger.lifecycle(
                    "Schema $schemaName is compatible with subject $subject. " +
                        "Compatibility: ${newCompatibility ?: originalCompatibility}",
                )
            }
        } finally {
            // Restore the original compatibility level even if the validation fails
            newCompatibility?.let { client.updateCompatibility(subject, originalCompatibility) }
        }
    }
}
