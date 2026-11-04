package io.github.rudikone.avroschemawizardplugin

import java.io.Serializable

/**
 * A flat, serializable representation of a single schema reference.
 * Mirrors [io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference],
 * safe for the Configuration Cache and for storage in the task state.
 */
data class SchemaReferenceSpec(
    val name: String,
    val subject: String,
    val version: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * A flat, serializable representation of a single subject config.
 * Contains no references to Project / Gradle-managed objects,
 * making it safe for the Configuration Cache and for storage in the task state.
 */
data class SubjectSpec(
    val topic: String,
    val searchAvroFilePath: String,
    val protocol: String?,
    val schema: String,
    val subjectNameStrategy: String,
    val references: List<SchemaReferenceSpec> = emptyList(),
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
