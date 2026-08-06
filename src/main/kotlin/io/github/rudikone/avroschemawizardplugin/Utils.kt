package io.github.rudikone.avroschemawizardplugin

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.serializers.subject.RecordNameStrategy
import io.confluent.kafka.serializers.subject.TopicNameStrategy
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy
import io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy
import org.apache.avro.Protocol
import org.apache.avro.Schema
import org.gradle.api.logging.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

const val AVPR = "avpr"
const val AVSC = "avsc"

fun buildFileCache(specs: Collection<SubjectSpec>): Map<String, File> {
    val uniquePaths = specs.map { it.searchAvroFilePath }.distinct()
    val fileCache = mutableMapOf<String, File>()

    uniquePaths.forEach { path ->
        val dir = Paths.get(path)
        require(Files.isDirectory(dir)) {
            "Avro search path does not exist or is not a directory: $path"
        }
        Files.walk(dir).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .filter { file -> file.extension == AVSC || file.extension == AVPR }
                .forEach { file ->
                    fileCache[file.fileName.toString()] = file.toFile()
                }
        }
    }

    return fileCache
}

fun generateSchema(
    spec: SubjectSpec,
    fileCache: Map<String, File>,
): AvroSchema {
    val fileName = spec.protocol ?: spec.schema
    val avroFile =
        fileCache["$fileName.$AVPR"]
            ?: fileCache["$fileName.$AVSC"]
            ?: error("File $fileName not found in configured search path(s)!")

    return avroFile.inputStream().use { fis ->
        if (avroFile.extension == AVSC) {
            AvroSchema(Schema.Parser().parse(fis))
        } else {
            AvroSchema(Protocol.parse(fis).getType(spec.schema))
        }
    }
}

fun String.toSubjectNameStrategy(): SubjectNameStrategy =
    when (SubjectNameStrategies.from(this)) {
        SubjectNameStrategies.TopicNameStrategy -> TopicNameStrategy()
        SubjectNameStrategies.RecordNameStrategy -> RecordNameStrategy()
        SubjectNameStrategies.TopicRecordNameStrategy -> TopicRecordNameStrategy()
        null -> error("Unsupported subject name strategy. Allowed: ${SubjectNameStrategies.values().joinToString()}")
    }

fun logStart(logger: Logger) {
    logger.lifecycle(
        """
           _               __      ___                _
          /_\__ ___ _ ___  \ \    / (_)_____ _ _ _ __| |
         / _ \ V / '_/ _ \  \ \/\/ /| |_ / _` | '_/ _` |
        /_/ \_\_/|_| \___/   \_/\_/ |_/__\__,_|_| \__,_|
        """.trimIndent(),
    )
}
