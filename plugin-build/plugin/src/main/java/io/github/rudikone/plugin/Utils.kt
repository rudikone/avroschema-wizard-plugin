package io.github.rudikone.plugin

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.jvm.optionals.getOrNull

fun findAvroFileByName(
    searchPaths: Set<String>,
    schemaName: String,
): File {
    var file: File? = null

    searchPaths.forEach { path ->
        if (file == null) {
            file =
                Files.walk(Paths.get(path)).use {
                    it.filter { file -> file.isRegularFile() && file.fileName.toString() == "$schemaName.avsc" }
                        .findFirst()
                        .getOrNull()
                        ?.toFile()
                }
        }
    }

    return file ?: error("File $schemaName.avsc not found!")
}
