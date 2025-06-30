package io.github.rudikone.avroschemawizardplugin

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.confluent.kafka.serializers.subject.TopicNameStrategy
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy
import org.apache.avro.Protocol
import org.apache.avro.Schema
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.jvm.optionals.getOrNull

const val AVPR = "avpr"
const val AVSC = "avsc"

fun generateSchema(config: SubjectConfig): AvroSchema {
    val fileName = config.protocol.orNull ?: config.schema.get()
    val avroFile = findAvroFileByName(path = config.searchAvroFilePath.get(), name = fileName)

    return FileInputStream(avroFile).use { fis ->
        if (avroFile.extension == AVSC) {
            AvroSchema(Schema.Parser().parse(fis))
        } else {
            AvroSchema(Protocol.parse(fis).getType(config.schema.get()))
        }
    }
}

private fun findAvroFileByName(
    path: String,
    name: String,
): File =
    Files.walk(Paths.get(path)).use {
        it.filter { file ->
            file.isRegularFile() &&
                (file.fileName.toString() == "$name.$AVPR" || file.fileName.toString() == "$name.$AVSC")
        }.findFirst().getOrNull()?.toFile()
    } ?: error("File $name not found!")

fun String.toSubjectNameStrategy(): SubjectNameStrategy =
    when (SubjectNameStrategies.from(this)) {
        SubjectNameStrategies.TopicNameStrategy -> TopicNameStrategy()
        SubjectNameStrategies.RecordNameStrategy -> RecordNameStrategy()
        SubjectNameStrategies.TopicRecordNameStrategy -> TopicRecordNameStrategy()
        null ->
            error(
                "Unsupported subject name strategy. Allowed: ${SubjectNameStrategies.values().joinToString()}",
            )
    }
