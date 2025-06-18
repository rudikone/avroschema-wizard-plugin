package io.github.rudikone.avroschemawizardplugin

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.jvm.optionals.getOrNull

const val AVPR = "avpr"
const val AVSC = "avsc"

fun findAvroFileByName(
    path: String,
    name: String,
): File {
    val file: File? =
        Files.walk(Paths.get(path)).use {
            it.filter { file ->
                file.isRegularFile() &&
                    (file.fileName.toString() == "$name.$AVPR" || file.fileName.toString() == "$name.$AVSC")
            }
                .findFirst()
                .getOrNull()
                ?.toFile()
        }

    return file ?: error("File $name not found!")
}
