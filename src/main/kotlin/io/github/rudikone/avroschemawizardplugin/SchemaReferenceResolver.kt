package io.github.rudikone.avroschemawizardplugin

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import java.io.IOException

/**
 * Resolves schema references by fetching the referenced schema (subject + version) from
 * Schema Registry and keying it by the reference's fully-qualified name.
 */
internal class SchemaReferenceResolver(
    private val client: SchemaRegistryClient,
) {
    fun resolveReferences(references: List<SchemaReference>): Map<String, String> {
        validate(references)
        return references.associate { ref -> ref.name to fetchSchema(ref) }
    }

    private fun fetchSchema(ref: SchemaReference): String =
        try {
            client.getSchemaMetadata(ref.subject, ref.version).schema
        } catch (e: RestClientException) {
            throw resolutionError(ref, e)
        } catch (e: IOException) {
            throw resolutionError(ref, e)
        }

    private fun validate(references: List<SchemaReference>) {
        references.forEach { ref ->
            require(ref.name.isNotBlank()) {
                "Schema reference must have a non-blank 'name' (fully-qualified Avro type name)"
            }
            require(ref.version > 0) {
                "Schema reference '${ref.name}' must have a positive version, got ${ref.version}"
            }
        }
        val duplicates =
            references.groupBy { it.name }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Duplicate schema reference names are not allowed: $duplicates"
        }
    }

    private fun resolutionError(
        ref: SchemaReference,
        e: Exception,
    ): IllegalStateException =
        IllegalStateException(
            "Cannot resolve reference '${ref.name}' for subject '${ref.subject}' " +
                "version ${ref.version}: ${e.message}",
            e,
        )
}
