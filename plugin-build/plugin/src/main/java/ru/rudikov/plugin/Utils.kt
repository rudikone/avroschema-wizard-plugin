package ru.rudikov.plugin

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.jvm.optionals.getOrNull

fun findAvroFileByName(
    searchPath: String,
    schemaName: String,
) = Files.walk(Paths.get(searchPath)).use {
    it.filter { file -> file.isRegularFile() && file.fileName.toString() == "$schemaName.avsc" }
        .findFirst()
        .getOrNull()
        ?.toFile()
        ?: error("File $schemaName.avsc not found!")
}
