@file:Suppress("MaxLineLength")

package io.github.rudikone.avroschemawizardplugin.testutils

class Plugin(
    private val id: String,
    private val version: String? = null,
    private val apply: Boolean? = null,
) {
    override fun toString(): String = "id(\"$id\") ${getVersion()} ${getApply()}"

    private fun getVersion() = if (version == null) "" else "version \"$version\""

    private fun getApply() = if (apply == null) "" else "apply $apply"
}

private const val DEFAULT_KOTLIN_VERSION = "1.9.20"

// See build.gradle.kts tasks.test
fun avroSchemaWizard(version: String? = System.getenv("avroschema-wizard-plugin-version")) =
    Plugin("io.github.rudikone.avroschema-wizard-plugin", version)

fun kotlinJvm(version: String? = DEFAULT_KOTLIN_VERSION) = Plugin("org.jetbrains.kotlin.jvm", version)
