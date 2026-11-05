package io.github.rudikone.avroschemawizardplugin

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaMetadata
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

@Suppress("ktlint:standard:function-signature")
class SchemaReferenceResolverTest {
    @Test
    fun `resolves references into a name to schema map`() {
        val sharedSchema =
            """
            {
                "type": "record",
                "namespace": "ru.rudikov.reference",
                "name": "SharedRecord",
                "fields": [ { "name": "id", "type": "int" } ]
            }
            """.trimIndent()
        val client = StubSchemaRegistryClient(mapOf("shared-value" to 1 to sharedSchema))
        val references =
            listOf(SchemaReference("ru.rudikov.reference.SharedRecord", "shared-value", 1))

        val resolved = SchemaReferenceResolver(client).resolveReferences(references)

        assertEquals(mapOf("ru.rudikov.reference.SharedRecord" to sharedSchema), resolved)
    }

    @Test
    fun `throws clear error naming the reference when subject is unknown`() {
        val client = StubSchemaRegistryClient(emptyMap())
        val references =
            listOf(SchemaReference("ru.rudikov.reference.SharedRecord", "missing-value", 1))

        val message =
            try {
                SchemaReferenceResolver(client).resolveReferences(references)
                error("Expected an IllegalStateException")
            } catch (e: IllegalStateException) {
                e.message
            }
        assertTrue(
            "Cannot resolve reference 'ru.rudikov.reference.SharedRecord' " +
                "for subject 'missing-value' version 1" in message!!,
            "Expected a clear resolution error.\nActual: $message",
        )
    }

    private class StubSchemaRegistryClient(
        private val schemasByKey: Map<Pair<String, Int>, String>,
    ) : SchemaRegistryClient {
        private fun unsupported(): Nothing = throw UnsupportedOperationException()

        override fun parseSchema(
            subject: String,
            schemaString: String,
            references: MutableList<SchemaReference>,
        ): Optional<ParsedSchema> = unsupported()

        override fun register(subject: String, schema: ParsedSchema): Int = unsupported()

        override fun register(subject: String, schema: ParsedSchema, version: Int, id: Int): Int =
            unsupported()

        override fun getSchemaById(id: Int): ParsedSchema = unsupported()

        override fun getSchemaBySubjectAndId(subject: String, id: Int): ParsedSchema =
            unsupported()

        override fun getAllSubjectsById(id: Int): MutableCollection<String> = unsupported()

        override fun getLatestSchemaMetadata(subject: String): SchemaMetadata = unsupported()

        override fun getSchemaMetadata(subject: String, version: Int): SchemaMetadata {
            val schema =
                schemasByKey[subject to version]
                    ?: throw RestClientException(
                        "Subject '$subject' version $version not found",
                        40401,
                        404,
                    )
            return SchemaMetadata(1, version, schema)
        }

        override fun getVersion(subject: String, schema: ParsedSchema): Int = unsupported()

        override fun getAllVersions(subject: String): MutableList<Int> = unsupported()

        override fun testCompatibility(subject: String, schema: ParsedSchema): Boolean =
            unsupported()

        override fun setMode(mode: String): String = unsupported()

        override fun setMode(subject: String, mode: String): String = unsupported()

        override fun getMode(): String = unsupported()

        override fun getMode(subject: String): String = unsupported()

        override fun getAllSubjects(): MutableCollection<String> = unsupported()

        override fun getId(subject: String, schema: ParsedSchema): Int = unsupported()

        override fun reset() = unsupported()
    }
}
